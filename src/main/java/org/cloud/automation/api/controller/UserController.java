package org.cloud.automation.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cloud.automation.api.dto.CreateUserRequest;
import org.cloud.automation.api.dto.ErrorResponse;
import org.cloud.automation.api.dto.UserDto;
import org.cloud.automation.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST-контроллер для управления пользователями (Users).
 * <p>
 * Предоставляет эндпоинты для выполнения CRUD-операций над пользователями,
 * которые могут инициировать выполнение задач.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for creating, retrieving, updating, and deleting users.")
public class UserController {

    private final UserService userService;

    /**
     * Получает список всех пользователей.
     * @return Ответ 200 OK со списком DTO пользователей.
     */
    @GetMapping
    @Operation(summary = "{api.user.getAll.summary}", description = "{api.user.getAll.description}")
    @ApiResponse(responseCode = "200", description = "{api.response.ok}")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Получает информацию о пользователе по его ID.
     * @param id Уникальный идентификатор пользователя.
     * @return Ответ 200 OK с DTO пользователя или 404 Not Found.
     */
    @GetMapping("/{id}")
    @Operation(summary = "{api.user.getById.summary}", description = "{api.user.getById.description}")
    @ApiResponse(responseCode = "200", description = "{api.response.ok}")
    @ApiResponse(responseCode = "404", description = "{api.response.notfound}",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UserDto> getUserById(
            @Parameter(description = "{api.id.parameter.description}") @PathVariable Long id) {
        return userService.findUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Создает нового пользователя.
     * @param createUserRequest DTO с данными для создания.
     * @return Ответ 201 Created с созданным пользователем.
     */
    @PostMapping
    @Operation(summary = "{api.user.create.summary}", description = "{api.user.create.description}")
    @ApiResponse(responseCode = "201", description = "{api.response.created}")
    @ApiResponse(responseCode = "400", description = "{api.response.badrequest}",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "{api.response.conflict}",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        UserDto userDto = UserDto.builder()
                .name(createUserRequest.getName())
                .email(createUserRequest.getEmail())
                .role(createUserRequest.getRole())
                .build();

        UserDto createdUser = userService.createUser(userDto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUser.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdUser);
    }

    /**
     * Обновляет существующего пользователя.
     * @param id ID пользователя для обновления.
     * @param userDto DTO с новыми данными.
     * @return Ответ 200 OK с обновленным DTO пользователя или 404 Not Found.
     */
    @PutMapping("/{id}")
    @Operation(summary = "{api.user.update.summary}", description = "{api.user.update.description}")
    @ApiResponse(responseCode = "200", description = "{api.response.ok}")
    @ApiResponse(responseCode = "400", description = "{api.response.badrequest}",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "{api.response.notfound}",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UserDto> updateUser(
            @Parameter(description = "{api.id.parameter.description}") @PathVariable Long id,
            @Valid @RequestBody UserDto userDto) {
        return userService.updateUser(id, userDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет пользователя по ID.
     * @param id ID пользователя для удаления.
     * @return Ответ 204 No Content в случае успеха или 404 Not Found.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "{api.user.delete.summary}", description = "{api.user.delete.description}")
    @ApiResponse(responseCode = "204", description = "{api.response.ok}")
    @ApiResponse(responseCode = "404", description = "{api.response.notfound}",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "{api.id.parameter.description}") @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
