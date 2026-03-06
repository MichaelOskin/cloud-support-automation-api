package org.cloud.automation.api.service;

import org.cloud.automation.api.domain.AuditEvent;
import org.cloud.automation.api.domain.Task;

import java.util.Map;

/**
 * Сервис для записи событий аудита.
 */
public interface AuditService {

    /**
     * Создает и сохраняет запись в журнале аудита.
     *
     * @param task    Задача, с которой связано событие.
     * @param event   Тип события.
     * @param details Дополнительная информация о событии (может быть null).
     */
    void logEvent(Task task, AuditEvent event, Map<String, Object> details);

}
