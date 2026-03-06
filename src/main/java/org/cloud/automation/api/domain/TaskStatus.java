package org.cloud.automation.api.domain;

/**
 * Перечисление, определяющее возможные статусы жизненного цикла задачи {@link org.cloud.automation.api.domain.Task}.
 */
public enum TaskStatus {
    /**
     * Задача создана и ожидает обработки.
     */
    PENDING,

    /**
     * Задача находится в процессе выполнения.
     */
    RUNNING,

    /**
     * Задача была успешно выполнена.
     */
    SUCCESS,

    /**
     * Выполнение задачи завершилось с ошибкой.
     */
    FAILED
}
