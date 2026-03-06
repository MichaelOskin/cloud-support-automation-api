package org.cloud.automation.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.cloud.automation.api.domain.TaskStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object (DTO) для представления задачи (Task) в ответах API.
 * <p>
 * Этот класс используется для передачи полной информации о задаче клиенту,
 * скрывая при этом детали реализации JPA-сущности.
 */
@Data
@Builder
@Schema(description = "Полное представление асинхронной задачи")
public class TaskDto {

    @Schema(description = "Уникальный идентификатор задачи.", example = "1")
    private Long id;

    @Schema(description = "Тип задачи.", example = "ssh_command")
    private String type;

    @Schema(description = "Текущий статус выполнения задачи.")
    private TaskStatus status;

    @Schema(description = "Параметры, с которыми была запущена задача.", example = "{\"host\":\"1.2.3.4\", \"command\":\"df -h\"}")
    private Map<String, Object> parameters;

    @Schema(description = "Результат успешного выполнения задачи.", example = "{\"exitCode\":0, \"stdout\":\"Filesystem Size Used...\"}")
    private Map<String, Object> result;

    @Schema(description = "Сообщение об ошибке, если задача провалилась.", example = "Connection timed out")
    private String errorMessage;

    @Schema(description = "Время создания задачи.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Europe/Moscow")
    private LocalDateTime createdAt;

    @Schema(description = "Время последнего обновления задачи.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Europe/Moscow")
    private LocalDateTime updatedAt;

    @Schema(description = "Время завершения задачи.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Europe/Moscow")
    private LocalDateTime completedAt;

    @Schema(description = "ID пользователя, который инициировал задачу.", example = "101")
    private Long userId;

}
