# Spring Boot CI/CD Demo — Jenkins Pipeline

A production-ready Spring Boot REST API demonstrating a full Jenkins CI/CD pipeline with Docker.

## Project Structure

```
springboot-cicd/
├── src/
│   ├── main/java/com/example/demo/
│   │   ├── DemoApplication.java          # Entry point
│   │   ├── controller/
│   │   │   ├── EmployeeController.java   # REST endpoints
│   │   │   └── HealthController.java     # Health check
│   │   ├── service/
│   │   │   └── EmployeeService.java      # Business logic
│   │   ├── repository/
│   │   │   └── EmployeeRepository.java   # Data access
│   │   └── model/
│   │       └── Employee.java             # Entity
│   ├── main/resources/
│   │   └── application.properties
│   └── test/java/com/example/demo/
│       ├── EmployeeServiceTest.java      # Unit tests (Mockito)
│       └── EmployeeControllerIntegrationTest.java  # Integration tests
├── Dockerfile                            # Multi-stage Docker build
├── Jenkinsfile                           # CI/CD Pipeline definition
└── pom.xml                              # Maven build config
```

## API Endpoints

| Method | Endpoint                        | Description            |
|--------|---------------------------------|------------------------|
| GET    | /api/health                     | App health check       |
| GET    | /api/employees                  | Get all employees      |
| GET    | /api/employees/{id}             | Get employee by ID     |
| POST   | /api/employees                  | Create employee        |
| PUT    | /api/employees/{id}             | Update employee        |
| DELETE | /api/employees/{id}             | Delete employee        |
| GET    | /api/employees/department/{dept}| Filter by department   |

## Jenkins Pipeline Stages

1. **Checkout**         — Pull source code from Git/GitHub
2. **Build**           — `mvn clean compile`
3. **Unit Tests**      — `mvn test` + JUnit & JaCoCo reports
4. **Code Quality**    — SonarQube static analysis (main/develop only)
5. **Quality Gate**    — Block pipeline if code quality fails
6. **Package**         — `mvn package` → JAR artifact
7. **Docker Build**    — Build & tag Docker image
8. **Docker Push**     — Push to Docker Hub (main branch only)
9. **Deploy Staging**  — Run container on port 8081
10. **Smoke Test**     — Curl health endpoint to verify
11. **Deploy Prod**    — Manual approval gate → production

## Jenkins Setup

### Prerequisites
- Jenkins 2.400+ with plugins: Pipeline, JUnit, JaCoCo, SonarQube Scanner, Docker Pipeline, Email Extension
- Docker installed on the Jenkins agent
- JDK 17 and Maven 3.9 configured in Jenkins Global Tool Configuration

### Configure Jenkins Tools
Go to **Manage Jenkins → Global Tool Configuration**:
- Add JDK: name = `JDK17`
- Add Maven: name = `Maven3`

### Configure Credentials
Go to **Manage Jenkins → Credentials**:
- Add `dockerhub-credentials` (Username/Password) for Docker Hub push

### Create Pipeline Job
1. New Item → Pipeline
2. Set **Pipeline Definition** = "Pipeline script from SCM"
3. SCM = Git, enter your repo URL
4. Script Path = `Jenkinsfile`

## Run Locally

```bash
# Build
mvn clean package

# Run
java -jar target/demo-1.0.0.jar

# Or with Docker
docker build -t springboot-cicd-demo .
docker run -p 8080:8080 springboot-cicd-demo
```

Test the health check:
```bash
curl http://localhost:8080/api/health
```
