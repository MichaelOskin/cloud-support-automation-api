package org.cloud.automation.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.cloud.automation.api.domain.UserRole;

/**
 * Data Transfer Object (DTO) для создания нового пользователя.
 * Используется в качестве тела запроса для эндпоинта POST /api/v1/users.
 */
@Data
@Builder
@Schema(description = "Запрос на создание нового пользователя")
public class CreateUserRequest {

    @NotBlank(message = "Name cannot be blank")
    @Schema(description = "Имя пользователя.", requiredMode = Schema.RequiredMode.REQUIRED, example = "Jane Doe")
    private String name;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email cannot be blank")
    @Schema(description = "Электронная почта (должна быть уникальной).", requiredMode = Schema.RequiredMode.REQUIRED, example = "jane.doe@example.com")
    private String email;

    @NotNull(message = "Role cannot be null")
    @Schema(description = "Роль пользователя.", requiredMode = Schema.RequiredMode.REQUIRED, example = "AGENT")
    private UserRole role;
}
