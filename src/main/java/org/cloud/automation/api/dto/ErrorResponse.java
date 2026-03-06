package org.cloud.automation.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) для стандартизированного представления ошибок в API.
 * Используется в {@link org.cloud.automation.api.controller.advice.GlobalExceptionHandler}.
 */
@Data
@Builder
@AllArgsConstructor
@Schema(description = "Стандартизированный ответ с информацией об ошибке")
public class ErrorResponse {

    @Schema(description = "Временная метка возникновения ошибки.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP статус код.", example = "404")
    private int status;

    @Schema(description = "Краткое описание статуса HTTP.", example = "Not Found")
    private String error;

    @Schema(description = "Детальное сообщение об ошибке.", example = "Task with ID 99 not found.")
    private String message;

    @Schema(description = "Путь, по которому был сделан запрос.", example = "/api/v1/tasks/99")
    private String path;

}
