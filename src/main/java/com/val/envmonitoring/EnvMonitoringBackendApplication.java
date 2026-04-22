package com.val.envmonitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EnvMonitoringBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnvMonitoringBackendApplication.class, args);
    }
}