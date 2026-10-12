package com.agroverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AgroverseApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgroverseApplication.class, args);
    }
}
