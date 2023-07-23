package com.esmile.spring.validation;

import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;


/**
 * @Description
 * https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.validation
 * @Date 12/7/2023
 * @Created by nisran
 */
@Validated
@Component
public class MethodValidation {

    public void  findByCodeAndAuthor(@Size(min = 8, max = 10) String code, String author) {
    }

}
