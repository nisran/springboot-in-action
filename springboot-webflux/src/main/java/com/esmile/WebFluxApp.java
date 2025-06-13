package com.esmile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class WebFluxApp {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(WebFluxApp.class, args);
    }

}