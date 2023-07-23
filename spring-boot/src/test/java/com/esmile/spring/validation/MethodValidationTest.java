package com.esmile.spring.validation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Classname MethodValidationTest
 * @Description TODO
 * @Version 1.0.0
 * @Date 13/7/2023
 * @Created by nisran
 */
@SpringBootTest
class MethodValidationTest {

    @Autowired
    private MethodValidation methodValidation;

    @Test
    void findByCodeAndAuthor() {
        methodValidation.findByCodeAndAuthor("1","dam");
    }
}
