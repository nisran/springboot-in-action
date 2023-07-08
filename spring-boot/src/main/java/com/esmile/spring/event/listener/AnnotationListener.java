package com.esmile.spring.event.listener;

import com.esmile.spring.event.CustomEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @Date 6/7/2023 下午7:52
 * @Created by nisran
 */
@Slf4j
@Component
public class AnnotationListener {

    @EventListener
    public void listen(CustomEvent customEvent){
        log.info("Listen Custom Event, message: {}", customEvent.getMessage());
    }
}
