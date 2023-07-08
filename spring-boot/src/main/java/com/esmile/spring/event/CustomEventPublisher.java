package com.esmile.spring.event;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;


/**
 * @Date 4/7/2023
 * @Created by nisran
 */
@Slf4j
@Data
@Component
public class CustomEventPublisher {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public void publishCustomEvent(String message){
        log.info("Publishing custom event...");
        applicationEventPublisher.publishEvent(new CustomEvent(this, message));
        log.info("Publishing custom event completed.");
    }

}
