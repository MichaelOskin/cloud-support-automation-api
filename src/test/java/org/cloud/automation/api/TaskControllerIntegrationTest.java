package org.cloud.automation.api;

import org.cloud.automation.api.domain.TaskStatus;
import org.cloud.automation.api.dto.CreateTaskRequest;
import org.cloud.automation.api.dto.TaskDto;
import org.cloud.automation.api.repository.TaskRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TaskControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TaskRepository taskRepository;

    @Value("${test.ssh.host}")
    private String sshHost;

    @Value("${test.ssh.port}")
    private int sshPort;

    @Value("${test.ssh.user}")
    private String sshUser;

    @Value("${test.ssh.password}")
    private String sshPassword;
    
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
        registry.add("test.ssh.password", () -> "testpassword"); // It's better to get this from a constant
    }


    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void testCreateSshTask_Success() {
        // 1. Создаем задачу
        Map<String, Object> params = Map.of(
                "host", sshHost,
                "port", sshPort,
                "username", sshUser,
                "password", sshPassword,
                "command", "echo 'hello ssh'"
        );
        CreateTaskRequest request = CreateTaskRequest.builder()
                .type("ssh_command")
                .parameters(params)
                .build();

        ResponseEntity<TaskDto> createResponse = restTemplate.postForEntity("/api/v1/tasks", request, TaskDto.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getStatus()).isEqualTo(TaskStatus.PENDING);
        Long taskId = createResponse.getBody().getId();

        // 2. Ждем выполнения задачи
        await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
            ResponseEntity<TaskDto> getResponse = restTemplate.getForEntity("/api/v1/tasks/" + taskId, TaskDto.class);
            assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(getResponse.getBody()).isNotNull();
            assertThat(getResponse.getBody().getStatus()).isIn(TaskStatus.SUCCESS, TaskStatus.FAILED);
        });

        // 3. Проверяем результат
        ResponseEntity<TaskDto> finalResponse = restTemplate.getForEntity("/api/v1/tasks/" + taskId, TaskDto.class);
        TaskDto completedTask = finalResponse.getBody();

        assertThat(completedTask.getStatus()).isEqualTo(TaskStatus.SUCCESS);
        assertThat(completedTask.getErrorMessage()).isNull();
        assertThat(completedTask.getResult()).isNotNull();
        assertThat(completedTask.getResult().get("exitCode")).isEqualTo(0);
        assertThat(completedTask.getResult().get("stdout")).isEqualTo("hello ssh");
    }

    @Test
    void testCreateEc2Task_Success() {
        // 1. Создаем задачу
        Map<String, Object> params = Map.of(
                "imageId", "ami-12345", // LocalStack не валидирует ID
                "instanceType", "t2.micro",
                "keyName", "test-key",
                "securityGroupIds", List.of("sg-12345"),
                "subnetId", "subnet-12345"
        );
        CreateTaskRequest request = CreateTaskRequest.builder()
                .type("create_ec2")
                .parameters(params)
                .build();

        ResponseEntity<TaskDto> createResponse = restTemplate.postForEntity("/api/v1/tasks", request, TaskDto.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertNotNull(createResponse.getBody());
        Long taskId = createResponse.getBody().getId();

        // 2. Ждем выполнения
        await().atMost(60, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).untilAsserted(() -> {
            ResponseEntity<TaskDto> getResponse = restTemplate.getForEntity("/api/v1/tasks/" + taskId, TaskDto.class);
            assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            Assertions.assertNotNull(getResponse.getBody());
            assertThat(getResponse.getBody().getStatus()).isIn(TaskStatus.SUCCESS, TaskStatus.FAILED);
        });

        // 3. Проверяем результат
        ResponseEntity<TaskDto> finalResponse = restTemplate.getForEntity("/api/v1/tasks/" + taskId, TaskDto.class);
        TaskDto completedTask = finalResponse.getBody();

        assertThat(completedTask.getStatus()).isEqualTo(TaskStatus.SUCCESS);
        assertThat(completedTask.getResult().get("instanceId")).isNotNull();
        assertThat(completedTask.getResult().get("instanceId").toString()).startsWith("i-");
    }
}
