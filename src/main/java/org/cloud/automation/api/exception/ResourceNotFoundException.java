package org.cloud.automation.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Кастомное исключение, выбрасываемое, когда запрашиваемый ресурс не найден в системе.
 * <p>
 * Это исключение перехватывается в {@link org.cloud.automation.api.controller.advice.GlobalExceptionHandler},
 * который формирует и возвращает клиенту стандартизированный ответ с HTTP-статусом 404 Not Found.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Конструктор исключения.
     * @param message Сообщение об ошибке, которое будет показано клиенту.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
