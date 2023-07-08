package com.esmile.spring.event.listener;

import com.esmile.spring.event.CustomEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @Date 4/7/2023
 * @Created by nisran
 */
@Slf4j
@Component
public class CustomListener implements ApplicationListener<CustomEvent> {

    @Override
    public void onApplicationEvent(CustomEvent event) {
        log.info("listen custom event, message: {}", event.getMessage());
    }
}
