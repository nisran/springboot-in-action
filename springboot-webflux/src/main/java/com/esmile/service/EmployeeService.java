package com.esmile.service;

import com.esmile.domain.Employee;
import com.esmile.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // CREATE
    public Mono<Employee> createEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    // READ
    public Flux<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Mono<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    public Flux<Employee> getEmployeesByLevel(Integer level) {
        return employeeRepository.findByLevel(level);
    }

    public Flux<Employee> getEmployeesByDepartmentAndLevel(String department, Integer level) {
        return employeeRepository.findByDepartmentAndLevel(department, level);
    }

    // UPDATE
    public Mono<Employee> updateEmployee(Long id, Employee employee) {
        return employeeRepository.findById(id)
                .flatMap(existingEmployee -> {
                    existingEmployee.setName(employee.getName());
                    existingEmployee.setDepartment(employee.getDepartment());
                    existingEmployee.setLevel(employee.getLevel());
                    return employeeRepository.save(existingEmployee);
                });
    }

    // DELETE
    public Mono<Void> deleteEmployee(Long id) {
        return employeeRepository.deleteById(id);
    }
}