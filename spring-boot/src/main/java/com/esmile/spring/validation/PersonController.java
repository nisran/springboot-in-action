package com.esmile.spring.validation;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description TODO
 * @Date 12/7/2023
 * @Created by nisran
 */
@Slf4j
@RestController
@RequestMapping("/person")
public class PersonController {

    @PostMapping
    public void checkAge(@RequestBody @Valid PersonForm personForm) {
        log.info(personForm.toString());
        final Integer age = personForm.getAge();
        return;
    }
}
