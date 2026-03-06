package org.cloud.automation.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CloudSupportAutomationApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudSupportAutomationApiApplication.class, args);
    }

}
