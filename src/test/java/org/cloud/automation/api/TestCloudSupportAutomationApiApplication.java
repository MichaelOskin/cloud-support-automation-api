package org.cloud.automation.api;

import org.springframework.boot.SpringApplication;

public class TestCloudSupportAutomationApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(CloudSupportAutomationApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
