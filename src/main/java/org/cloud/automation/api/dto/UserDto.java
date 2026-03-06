package org.cloud.automation.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.cloud.automation.api.domain.UserRole;


/**
 * Data Transfer Object (DTO) для представления пользователя (User) в ответах API.
 */
@Data
@Builder
@Schema(description = "Представление пользователя в системе")
public class UserDto {

    @Schema(description = "Уникальный идентификатор пользователя.", example = "101")
    private Long id;

    @Schema(description = "Имя пользователя.", example = "John Doe")
    private String name;

    @Schema(description = "Электронная почта пользователя (уникальная).", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Роль пользователя в системе.")
    private UserRole role;

}
