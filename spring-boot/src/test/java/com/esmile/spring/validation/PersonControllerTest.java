package com.esmile.spring.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.SerializationUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

/**
 * @Classname PersonControllerTest
 * @Description TODO
 * @Version 1.0.0
 * @Date 12/7/2023
 * @Created by nisran
 */
@AutoConfigureMockMvc
@SpringBootTest
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void checkAge() throws Exception {
        final PersonForm personForm = new PersonForm();
        personForm.setAge(20);
        personForm.setName("Alfa");

        ObjectMapper mapper = new ObjectMapper();
        String jsonStr = mapper.writeValueAsString(personForm);

        byte[] bytes = SerializationUtils.serialize(personForm);

        MockHttpServletRequestBuilder createPerson = post("/person")
                .content(jsonStr)
                .contentType(MediaType.APPLICATION_JSON);


        final ResultActions perform = mockMvc.perform(createPerson);
    }
}
