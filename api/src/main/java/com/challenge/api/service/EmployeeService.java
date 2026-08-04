package com.challenge.api.service;

import com.challenge.api.dto.CreateEmployeeRequest;
import com.challenge.api.model.Employee;
import com.challenge.api.model.EmployeeImpl;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** Employee operations. No database is required, so employees are held in a map seeded at startup. */
@Service
public class EmployeeService {

    private static final List<CreateEmployeeRequest> SEED_EMPLOYEES = List.of(
            new CreateEmployeeRequest(
                    "Maya", "Fernandez", 142000, 38, "Principal Engineer", "maya.fernandez@example.com"),
            new CreateEmployeeRequest(
                    "Daniel", "Okafor", 118000, 31, "Senior Software Engineer", "daniel.okafor@example.com"),
            new CreateEmployeeRequest(
                    "Sofia", "Lindqvist", 97000, 27, "Software Engineer", "sofia.lindqvist@example.com"));

    // Concurrent because Spring handles requests on multiple threads.
    private final Map<UUID, Employee> employees = new ConcurrentHashMap<>();

    public EmployeeService() {
        SEED_EMPLOYEES.forEach(this::createEmployee);
    }

    public List<Employee> getAllEmployees() {
        return List.copyOf(employees.values());
    }

    /** Empty if no Employee has this UUID. The controller turns that into a 404. */
    public Optional<Employee> findByUuid(UUID uuid) {
        return Optional.ofNullable(employees.get(uuid));
    }

    public Employee createEmployee(CreateEmployeeRequest request) {
        Employee employee = toEmployee(request);
        employees.put(employee.getUuid(), employee);
        return employee;
    }

    // Fills in the fields the caller does not send. Termination date stays null, which the Employee
    // interface defines as still employed.
    private Employee toEmployee(CreateEmployeeRequest request) {
        EmployeeImpl employee = new EmployeeImpl();
        employee.setUuid(UUID.randomUUID());
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setFullName("%s %s".formatted(request.firstName(), request.lastName()));
        employee.setSalary(request.salary());
        employee.setAge(request.age());
        employee.setJobTitle(request.jobTitle());
        employee.setEmail(request.email());
        employee.setContractHireDate(Instant.now());
        return employee;
    }
}
