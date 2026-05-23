package com.example.demo;

import com.example.demo.model.Employee;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        employee = new Employee(1L, "Alice", "Engineering", "alice@example.com", "Developer");
    }

    @Test
    void testGetAllEmployees() {
        when(employeeRepository.findAll()).thenReturn(Arrays.asList(employee));
        List<Employee> result = employeeService.getAllEmployees();
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getName());
    }

    @Test
    void testGetEmployeeById_Found() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        Optional<Employee> result = employeeService.getEmployeeById(1L);
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getName());
    }

    @Test
    void testGetEmployeeById_NotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());
        Optional<Employee> result = employeeService.getEmployeeById(99L);
        assertFalse(result.isPresent());
    }

    @Test
    void testCreateEmployee() {
        when(employeeRepository.save(employee)).thenReturn(employee);
        Employee result = employeeService.createEmployee(employee);
        assertNotNull(result);
        assertEquals("Alice", result.getName());
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    void testDeleteEmployee_Exists() {
        when(employeeRepository.existsById(1L)).thenReturn(true);
        boolean result = employeeService.deleteEmployee(1L);
        assertTrue(result);
        verify(employeeRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteEmployee_NotExists() {
        when(employeeRepository.existsById(99L)).thenReturn(false);
        boolean result = employeeService.deleteEmployee(99L);
        assertFalse(result);
        verify(employeeRepository, never()).deleteById(any());
    }

    @Test
    void testGetEmployeesByDepartment() {
        when(employeeRepository.findByDepartment("Engineering"))
                .thenReturn(Arrays.asList(employee));
        List<Employee> result = employeeService.getEmployeesByDepartment("Engineering");
        assertEquals(1, result.size());
        assertEquals("Engineering", result.get(0).getDepartment());
    }
}
