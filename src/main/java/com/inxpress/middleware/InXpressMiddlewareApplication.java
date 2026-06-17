package com.inxpress.middleware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InXpressMiddlewareApplication {

    public static void main(String[] args) {
        SpringApplication.run(InXpressMiddlewareApplication.class, args);
    }
}
