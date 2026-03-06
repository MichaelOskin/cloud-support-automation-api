package org.cloud.automation.api;

import org.cloud.automation.api.dto.CreateUserRequest;
import org.cloud.automation.api.dto.UserDto;
import org.cloud.automation.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// This import is likely how Testcontainers are activated for the application context.
// It should point to a class that sets up the Testcontainer datasource.
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test") // Assuming 'test' profile is configured to use application-test.properties/yaml
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

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

    // We might need EntityManager or TransactionTemplate for more complex setups or cleanup,
    // but for simple deleteAll, the repository should suffice.
    // Relying on Testcontainers ddl-auto=create-drop for initial schema setup.

    @AfterEach
    void tearDown() {
        // Clean up the database after each test to ensure isolation.
        // This is crucial for ensuring tests don't interfere with each other.
        userRepository.deleteAll();
    }

    @Test
    void testCreateUser_Success() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .name("Test User Creation")
                .email("create.test@example.com")
                .role(org.cloud.automation.api.domain.UserRole.AGENT)
                .build();
        HttpEntity<CreateUserRequest> requestEntity = new HttpEntity<>(createUserRequest, headers);

        // Act
        ResponseEntity<UserDto> response = restTemplate.exchange(
                "/api/v1/users",
                HttpMethod.POST,
                requestEntity,
                UserDto.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Test User Creation");
        assertThat(response.getBody().getEmail()).isEqualTo("create.test@example.com");
        assertThat(response.getBody().getRole()).isEqualTo(org.cloud.automation.api.domain.UserRole.AGENT);

        // Verify persistence
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.findByEmail("create.test@example.com")).isPresent();
    }

    @Test
    void testGetUserById_Success() {
        // Arrange: Create a user first
        UserDto createdUser = createUser("Get User Test", "get.test@example.com", org.cloud.automation.api.domain.UserRole.CUSTOMER);

        // Act
        ResponseEntity<UserDto> response = restTemplate.getForEntity("/api/v1/users/{id}", UserDto.class, createdUser.getId());

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(createdUser.getId());
        assertThat(response.getBody().getName()).isEqualTo("Get User Test");
    }

    @Test
    void testGetAllUsers_Success() {
        // Arrange: Create a couple of users
        createUser("User 1", "user1@example.com", org.cloud.automation.api.domain.UserRole.AGENT);
        createUser("User 2", "user2@example.com", org.cloud.automation.api.domain.UserRole.CUSTOMER);

        // Act
        ResponseEntity<UserDto[]> response = restTemplate.getForEntity("/api/v1/users", UserDto[].class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void testUpdateUser_Success() {
        // Arrange: Create a user first
        UserDto createdUser = createUser("Update User Original", "update.test@example.com", org.cloud.automation.api.domain.UserRole.AGENT);

        // Act: Prepare update data
        UserDto updatedData = UserDto.builder()
                .id(createdUser.getId()) // Keep the same ID
                .name("Update User Modified")
                .email("update.test.modified@example.com")
                .role(org.cloud.automation.api.domain.UserRole.CUSTOMER)
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<UserDto> requestEntity = new HttpEntity<>(updatedData, headers);

        ResponseEntity<UserDto> response = restTemplate.exchange(
                "/api/v1/users/{id}",
                HttpMethod.PUT,
                requestEntity,
                UserDto.class,
                createdUser.getId()
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(createdUser.getId());
        assertThat(response.getBody().getName()).isEqualTo("Update User Modified");
        assertThat(response.getBody().getEmail()).isEqualTo("update.test.modified@example.com");
        assertThat(response.getBody().getRole()).isEqualTo(org.cloud.automation.api.domain.UserRole.CUSTOMER);

        // Verify persistence
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void testDeleteUser_Success() {
        // Arrange: Create a user first
        UserDto createdUser = createUser("Delete Me", "delete.test@example.com", org.cloud.automation.api.domain.UserRole.AGENT);

        // Act
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/users/{id}",
                HttpMethod.DELETE,
                null, // No body for delete request
                Void.class,
                createdUser.getId()
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify deletion
        assertThat(userRepository.count()).isEqualTo(0);
        assertThat(userRepository.findByEmail("delete.test@example.com")).isNotPresent();
    }

    @Test
    void testGetUserById_NotFound() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/users/999", String.class); // Use String to get raw body

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        // The GlobalExceptionHandler should format the body, let's check for a core message.
        assertThat(response.getBody()).contains("User not found with id: 999");
    }

    @Test
    void testCreateUser_InvalidInput() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .name("") // Invalid: blank name
                .email("invalid.email") // Invalid: not an email format
                .role(null) // Invalid: null role
                .build();
        HttpEntity<CreateUserRequest> requestEntity = new HttpEntity<>(createUserRequest, headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users",
                HttpMethod.POST,
                requestEntity,
                String.class // Use String to capture the JSON body
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Name cannot be blank");
        assertThat(response.getBody()).contains("Email should be valid");
        assertThat(response.getBody()).contains("Role cannot be null");
    }

    @Test
    void testCreateUser_DuplicateEmail() {
        // Arrange: Create a user first
        UserDto createdUser = createUser("Existing User", "duplicate.email@example.com", org.cloud.automation.api.domain.UserRole.AGENT);

        // Act: Try to create another user with the same email
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .name("Another User")
                .email("duplicate.email@example.com") // Duplicate email
                .role(org.cloud.automation.api.domain.UserRole.CUSTOMER)
                .build();
        HttpEntity<CreateUserRequest> requestEntity = new HttpEntity<>(createUserRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Assert
        // The exact status code might vary (e.g., 500 if DataIntegrityViolationException is not handled specifically by @ControllerAdvice).
        // We expect a client error, typically 400 or 409.
        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.CONFLICT); // Allowing for variation in mapping
        // We expect a specific error message for duplicate email. This message might need adjustment
        // depending on how the service or exception handler returns it.
        // Assuming a message like "Email address already exists" or similar.
        assertThat(response.getBody()).contains("Email address already exists");
    }

    // Helper method to create a user and return its DTO
    private UserDto createUser(String name, String email, org.cloud.automation.api.domain.UserRole role) {
        CreateUserRequest request = CreateUserRequest.builder()
                .name(name)
                .email(email)
                .role(role)
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<CreateUserRequest> requestEntity = new HttpEntity<>(request, headers);

        ResponseEntity<UserDto> response = restTemplate.exchange("/api/v1/users", HttpMethod.POST, requestEntity, UserDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }
}
