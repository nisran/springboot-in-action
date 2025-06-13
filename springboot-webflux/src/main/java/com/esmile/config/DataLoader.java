package com.esmile.config;

import com.esmile.domain.Employee;
import com.esmile.repository.EmployeeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;

@Configuration
public class DataLoader {

    @Bean
    @Profile("!prod")
    public CommandLineRunner loadData(EmployeeRepository repository) {
        return args -> {
            // Clear existing data
            repository.deleteAll()
                    .thenMany(
                            Flux.just(
                                            new Employee(null, "John Doe", "Engineering", 2),
                                            new Employee(null, "Jane Smith", "Marketing", 2),
                                            new Employee(null, "Alice Johnson", "Engineering", 1),
                                            new Employee(null, "Bob Wilson", "Sales", 2),
                                            new Employee(null, "Charlie Brown", "Engineering", 3)
                                    )
                                    .flatMap(repository::save)
                    )
                    .subscribe(employee -> System.out.println("Saved: " + employee));
        };
    }
}