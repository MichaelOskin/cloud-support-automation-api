package org.cloud.automation.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class CloudSupportAutomationApiApplicationTests {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestcontainersConfiguration.POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", TestcontainersConfiguration.POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", TestcontainersConfiguration.POSTGRES_CONTAINER::getPassword);

        registry.add("aws.region", TestcontainersConfiguration.LOCAL_STACK_CONTAINER::getRegion);
        registry.add("aws.endpoint-override", () -> TestcontainersConfiguration.LOCAL_STACK_CONTAINER.getEndpointOverride(LocalStackContainer.Service.EC2).toString());
        registry.add("aws.access-key-id", TestcontainersConfiguration.LOCAL_STACK_CONTAINER::getAccessKey);
        registry.add("aws.secret-access-key", TestcontainersConfiguration.LOCAL_STACK_CONTAINER::getSecretKey);

        registry.add("test.ssh.host", TestcontainersConfiguration.SSH_CONTAINER::getHost);
        registry.add("test.ssh.port", () -> TestcontainersConfiguration.SSH_CONTAINER.getMappedPort(2222).toString());
        registry.add("test.ssh.user", () -> "testuser");
        registry.add("test.ssh.password", () -> "testpassword");
    }

    @Test
    void contextLoads() {
    }

}
