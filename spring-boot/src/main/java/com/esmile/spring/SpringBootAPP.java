package com.esmile.spring;

import com.esmile.spring.event.CustomEventPublisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class SpringBootAPP {

	public static void main(String[] args) {
		final ConfigurableApplicationContext applicationContext = SpringApplication.run(SpringBootAPP.class, args);

		final CustomEventPublisher publisher = applicationContext.getBean(CustomEventPublisher.class);
		publisher.publishCustomEvent("Hello");
	}

}
