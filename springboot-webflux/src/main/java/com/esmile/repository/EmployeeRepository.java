package com.esmile.repository;

import com.esmile.domain.Employee;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface EmployeeRepository extends ReactiveCrudRepository<Employee, Long> {

    // Custom query to find employees by level (L2 data)
    Flux<Employee> findByLevel(Integer level);

    // Custom query with SQL
    @Query("SELECT * FROM employees WHERE department = :department AND level = :level")
    Flux<Employee> findByDepartmentAndLevel(String department, Integer level);

}