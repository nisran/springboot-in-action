package com.esmile.spring.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @Date 4/7/2023
 * @Created by nisran
 */
@Getter
public class CustomEvent extends ApplicationEvent {

    private String message;

    public CustomEvent(Object source, String message) {
        super(source);
        this.message = message;
    }

}
