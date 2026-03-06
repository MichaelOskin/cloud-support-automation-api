package org.cloud.automation.api.service;

import org.cloud.automation.api.domain.Task;
import org.cloud.automation.api.dto.CreateTaskRequest;
import org.cloud.automation.api.dto.TaskDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;

import java.util.Optional;

/**
 * Сервис-оркестратор для управления жизненным циклом задач ({@link Task}).
 * <p>
 * Отвечает за основную бизнес-логику: создание задач, их поиск и инициирование
 * асинхронной обработки.
 */
public interface TaskService {

    /**
     * Создает новую задачу, сохраняет ее в базу данных и инициирует ее асинхронное выполнение.
     * @param request DTO с параметрами для создания задачи.
     * @return DTO созданной задачи со статусом PENDING.
     */
    TaskDto createTask(CreateTaskRequest request);

    /**
     * Находит задачу по ее уникальному идентификатору.
     * @param id ID задачи.
     * @return {@link Optional}, содержащий DTO задачи, если она найдена.
     */
    Optional<TaskDto> findTaskById(Long id);

    /**
     * Возвращает постраничный список всех задач.
     * @param pageable Параметры пагинации.
     * @return {@link Page} с задачами.
     */
    Page<TaskDto> findAllTasks(Pageable pageable);

    /**
     * Асинхронно обрабатывает задачу с указанным ID.
     * <p>
     * Этот метод выполняется в отдельном потоке из пула "taskExecutor".
     * Его основная задача — найти подходящий {@link org.cloud.automation.api.processor.TaskProcessor}
     * и делегировать ему фактическое выполнение задачи.
     *
     * @param taskId ID задачи для обработки.
     */
    @Async("taskExecutor")
    void processTask(Long taskId);

}
