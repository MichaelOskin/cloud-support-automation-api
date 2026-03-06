package org.cloud.automation.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import java.util.stream.Stream;

/**
 * Основная конфигурация Testcontainers для интеграционных тестов.
 * <p>
 * Этот класс использует паттерн "singleton container" со статическими полями,
 * чтобы запустить необходимые Docker-контейнеры (PostgreSQL, LocalStack, OpenSSH)
 * один раз на весь тестовый набор, что значительно ускоряет выполнение тестов
 * и решает проблемы с порядком инициализации Spring.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final String SSH_PASSWORD = "testpassword";

    static final PostgreSQLContainer<?> POSTGRES_CONTAINER;
    static final LocalStackContainer LOCAL_STACK_CONTAINER;
    static final GenericContainer<?> SSH_CONTAINER;

    // Статический блок для инициализации и запуска контейнеров один раз.
    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"));

        LOCAL_STACK_CONTAINER = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
                .withServices(LocalStackContainer.Service.EC2);

        SSH_CONTAINER = new GenericContainer<>(DockerImageName.parse("linuxserver/openssh-server:latest"))
                .withExposedPorts(2222)
                .withEnv("PASSWORD_ACCESS", "true")
                .withEnv("USER_NAME", "testuser")
                .withEnv("USER_PASSWORD", SSH_PASSWORD);

        // Параллельный запуск для ускорения
        Stream.of(POSTGRES_CONTAINER, LOCAL_STACK_CONTAINER, SSH_CONTAINER).parallel().forEach(GenericContainer::start);
    }

    /**
     * Создает специальный бин Ec2Client для тестов, который указывает на LocalStack.
     * @return Клиент EC2, настроенный на LocalStack.
     */
    @Bean
    @Profile("test")
    public Ec2Client ec2Client() {
        return Ec2Client.builder()
                .endpointOverride(LOCAL_STACK_CONTAINER.getEndpointOverride(LocalStackContainer.Service.EC2))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCAL_STACK_CONTAINER.getAccessKey(), LOCAL_STACK_CONTAINER.getSecretKey())))
                .region(Region.of(LOCAL_STACK_CONTAINER.getRegion()))
                .build();
    }
}
