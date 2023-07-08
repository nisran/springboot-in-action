package com.esmile.spring.event.listener;

import com.esmile.spring.event.CustomEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;

/**
 * @Date 6/7/2023 下午8:27
 * @Created by nisran
 */
@Slf4j
public class AsyncListener implements ApplicationListener<CustomEvent> {

    @SneakyThrows
    @Override
    public void onApplicationEvent(CustomEvent event) {
        log.info("Start Listen...");
        log.info("listen custom event processing, message: {}", event.getMessage());
        Thread.sleep(100);
        log.info("Listen Completed");
    }

}
