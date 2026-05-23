pipeline {
    agent any

    // ── Environment Variables ───────────────────────────────────────────────
    environment {
        APP_NAME        = 'springboot-cicd-demo'
        DOCKER_IMAGE    = "your-dockerhub-username/${APP_NAME}"
        DOCKER_TAG      = "${BUILD_NUMBER}"                    // Jenkins build number as tag
        JAVA_HOME       = tool 'JDK17'                        // Jenkins Global Tool name
        SONAR_PROJECT   = 'springboot-cicd-demo'
    }

    // ── Tool Versions ───────────────────────────────────────────────────────
    tools {
        maven 'Maven3'      // Must match name configured in Jenkins > Global Tool Config
        jdk   'JDK17'
    }

    // ── Pipeline Options ────────────────────────────────────────────────────
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))  // Keep last 10 builds
        timeout(time: 30, unit: 'MINUTES')              // Kill if hung
        timestamps()                                     // Timestamps in console
        disableConcurrentBuilds()                        // One build at a time
    }

    // ── Trigger: Poll SCM every 5 mins (or use webhook) ────────────────────
    triggers {
        pollSCM('H/5 * * * *')
    }

    // ════════════════════════════════════════════════════════════════════════
    // STAGES
    // ════════════════════════════════════════════════════════════════════════
    stages {

        // ── Stage 1: Checkout ───────────────────────────────────────────────
        stage('Checkout') {
            steps {
                echo "📥 Checking out source code..."
                checkout scm
                // Print the commit hash for traceability
                sh 'git log --oneline -1'
            }
        }

        // ── Stage 2: Build ──────────────────────────────────────────────────
        stage('Build') {
            steps {
                echo "🔨 Building application..."
                sh 'mvn clean compile -B'
            }
            post {
                failure {
                    echo "❌ Build failed!"
                }
            }
        }

        // ── Stage 3: Unit Tests ─────────────────────────────────────────────
        stage('Unit Tests') {
            steps {
                echo "🧪 Running unit tests..."
                sh 'mvn test -B'
            }
            post {
                always {
                    // Publish JUnit test results
                    junit '**/target/surefire-reports/*.xml'
                    // Publish JaCoCo coverage report
                    jacoco(
                        execPattern:       '**/target/jacoco.exec',
                        classPattern:      '**/target/classes',
                        sourcePattern:     '**/src/main/java',
                        exclusionPattern:  '**/DemoApplication.class'
                    )
                }
                failure {
                    echo "❌ Unit tests failed!"
                }
            }
        }

        // ── Stage 4: Code Quality (SonarQube) ──────────────────────────────
        stage('Code Quality Analysis') {
            when {
                // Only run on main/develop branches to save time on feature branches
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                echo "🔍 Running SonarQube analysis..."
                withSonarQubeEnv('SonarQube') {    // 'SonarQube' = Jenkins SonarQube server name
                    sh """
                        mvn sonar:sonar \
                            -Dsonar.projectKey=${SONAR_PROJECT} \
                            -Dsonar.projectName='Spring Boot CI/CD Demo' \
                            -B
                    """
                }
            }
        }

        // ── Stage 5: Quality Gate ───────────────────────────────────────────
        stage('Quality Gate') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                echo "🚦 Checking SonarQube Quality Gate..."
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // ── Stage 6: Package ────────────────────────────────────────────────
        stage('Package') {
            steps {
                echo "📦 Packaging application JAR..."
                sh 'mvn package -DskipTests -B'
                // Archive the JAR as a Jenkins artifact
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        // ── Stage 7: Build Docker Image ─────────────────────────────────────
        stage('Docker Build') {
            steps {
                echo "🐳 Building Docker image: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                sh "docker tag  ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
            }
        }

        // ── Stage 8: Push Docker Image ──────────────────────────────────────
        stage('Docker Push') {
            when {
                branch 'main'    // Only push on main branch
            }
            steps {
                echo "📤 Pushing Docker image to registry..."
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',  // Jenkins credential ID
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh "echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin"
                    sh "docker push ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    sh "docker push ${DOCKER_IMAGE}:latest"
                }
            }
        }

        // ── Stage 9: Deploy to Staging ──────────────────────────────────────
        stage('Deploy to Staging') {
            when {
                branch 'main'
            }
            steps {
                echo "🚀 Deploying to Staging environment..."
                // Stop old container if running, then start new one
                sh """
                    docker stop ${APP_NAME}-staging || true
                    docker rm   ${APP_NAME}-staging || true
                    docker run -d \
                        --name ${APP_NAME}-staging \
                        -p 8081:8080 \
                        ${DOCKER_IMAGE}:${DOCKER_TAG}
                """
                // Wait for app to start
                sh 'sleep 15'
            }
        }

        // ── Stage 10: Smoke Test ────────────────────────────────────────────
        stage('Smoke Test (Staging)') {
            when {
                branch 'main'
            }
            steps {
                echo "💨 Running smoke test on staging..."
                retry(3) {
                    sh '''
                        STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/health)
                        if [ "$STATUS" != "200" ]; then
                            echo "❌ Smoke test failed! HTTP status: $STATUS"
                            exit 1
                        fi
                        echo "✅ Smoke test passed! HTTP 200 received."
                    '''
                }
            }
        }

        // ── Stage 11: Deploy to Production ─────────────────────────────────
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            // Manual approval gate before production deploy
            input {
                message "Deploy to Production?"
                ok "Yes, deploy!"
                submitter "admin,release-team"
            }
            steps {
                echo "🎯 Deploying to Production..."
                sh """
                    docker stop ${APP_NAME}-prod || true
                    docker rm   ${APP_NAME}-prod || true
                    docker run -d \
                        --name ${APP_NAME}-prod \
                        -p 8080:8080 \
                        --restart unless-stopped \
                        ${DOCKER_IMAGE}:${DOCKER_TAG}
                """
            }
        }

    } // end stages

    // ════════════════════════════════════════════════════════════════════════
    // POST ACTIONS (run after all stages)
    // ════════════════════════════════════════════════════════════════════════
    post {
        success {
            echo "✅ Pipeline succeeded! Build #${BUILD_NUMBER} deployed."
            // emailext(
            //     subject: "✅ Build SUCCESS: ${APP_NAME} #${BUILD_NUMBER}",
            //     body:    "Pipeline passed. View: ${BUILD_URL}",
            //     to:      "team@example.com"
            // )
        }
        failure {
            echo "❌ Pipeline failed at stage. Check logs."
            // emailext(
            //     subject: "❌ Build FAILED: ${APP_NAME} #${BUILD_NUMBER}",
            //     body:    "Pipeline failed. View: ${BUILD_URL}",
            //     to:      "team@example.com"
            // )
        }
        always {
            echo "🧹 Cleaning up workspace..."
            // Remove dangling Docker images to save disk space
            sh 'docker image prune -f || true'
            cleanWs()
        }
    }
}
