package com.esmile.spring.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Classname CustomEventPublisherTest
 * @Description TODO
 * @Version 1.0.0
 * @Date 11/7/2023
 * @Created by nisran
 */
@SpringBootTest
class CustomEventPublisherTest {

    @Autowired
    private CustomEventPublisher publisher;

    @Test
    void publishCustomEvent() {
        publisher.publishCustomEvent("Hello");
    }
}
