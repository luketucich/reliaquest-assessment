package com.challenge.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.challenge.api.dto.CreateEmployeeRequest;
import com.challenge.api.model.Employee;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmployeeServiceTest {

    // Seeding happens in the constructor, so each test starts from a fresh, known state.
    private final EmployeeService employeeService = new EmployeeService();

    private static CreateEmployeeRequest request() {
        return new CreateEmployeeRequest("Ada", "Byron", 100000, 30, "Software Engineer", "ada.byron@example.com");
    }

    @Test
    void seedsEmployeesOnStartup() {
        assertEquals(3, employeeService.getAllEmployees().size());
    }

    @Test
    void createAssignsTheFieldsTheCallerDoesNotSend() {
        Employee created = employeeService.createEmployee(request());

        assertNotNull(created.getUuid());
        assertEquals("Ada Byron", created.getFullName());
        assertNotNull(created.getContractHireDate());
        assertNull(created.getContractTerminationDate(), "a new employee has not been terminated");
    }

    @Test
    void createKeepsTheValuesTheCallerDidSend() {
        Employee created = employeeService.createEmployee(request());

        assertEquals("Ada", created.getFirstName());
        assertEquals(100000, created.getSalary());
        assertEquals("ada.byron@example.com", created.getEmail());
    }

    @Test
    void createdEmployeeIsRetrievableByUuid() {
        Employee created = employeeService.createEmployee(request());

        assertEquals(created, employeeService.findByUuid(created.getUuid()).orElseThrow());
        assertEquals(4, employeeService.getAllEmployees().size());
    }

    @Test
    void findByUuidIsEmptyForAnUnknownUuid() {
        assertTrue(employeeService.findByUuid(UUID.randomUUID()).isEmpty());
    }
}
