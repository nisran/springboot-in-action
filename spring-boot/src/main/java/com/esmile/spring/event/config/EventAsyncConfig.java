package com.esmile.spring.event.config;

import com.esmile.spring.event.listener.AsyncListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * @Description 手动配置SimpleApplicationEventMulticaster， 并将将 taskExecutor 注入其中。 这样就可以实现多线程
 * @Date 6/7/2023
 * @Created by nisran
 */
@Configuration
public class EventAsyncConfig {

    @Bean
    public ApplicationEventMulticaster applicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();

        eventMulticaster.addApplicationListener(new AsyncListener());
        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return eventMulticaster;
    }
}
