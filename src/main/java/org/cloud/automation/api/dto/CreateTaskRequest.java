package org.cloud.automation.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Data Transfer Object (DTO) для создания новой асинхронной задачи.
 * Используется в качестве тела запроса для эндпоинта POST /api/v1/tasks.
 */
@Data
@Builder
@Schema(description = "Запрос на создание новой асинхронной задачи")
public class CreateTaskRequest {

    /**
     * Тип создаваемой задачи. Определяет, какой процессор будет ее выполнять.
     */
    @NotBlank(message = "Task type cannot be blank")
    @Schema(description = "Тип задачи.", requiredMode = Schema.RequiredMode.REQUIRED, example = "create_ec2")
    private String type;

    /**
     * Параметры, необходимые для выполнения задачи.
     * Структура этого объекта зависит от типа задачи.
     */
    @NotNull(message = "Parameters cannot be null")
    @Schema(description = "Параметры для выполнения задачи.", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "{\"imageId\":\"ami-123\", \"instanceType\":\"t2.micro\"}")
    private Map<String, Object> parameters;

    /**
     * Опциональный ID пользователя, от имени которого создается задача.
     */
    @Schema(description = "ID пользователя, инициирующего задачу (опционально).", example = "101")
    private Long userId;
}
