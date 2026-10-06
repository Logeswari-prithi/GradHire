package com.gradhire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GradHireApplication {
    public static void main(String[] args) {
        SpringApplication.run(GradHireApplication.class, args);
    }
}
`