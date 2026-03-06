package org.cloud.automation.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cloud.automation.api.dto.CreateTaskRequest;
import org.cloud.automation.api.dto.ErrorResponse;
import org.cloud.automation.api.dto.TaskDto;
import org.cloud.automation.api.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST-контроллер для управления асинхронными задачами (Tasks).
 * <p>
 * Предоставляет эндпоинты для создания, получения и листинга задач в соответствии с ТЗ.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Management", description = "Endpoints for creating and monitoring asynchronous tasks.")
public class TaskController {

    private final TaskService taskService;

    /**
     * Создает новую асинхронную задачу.
     * <p>
     * Принимает запрос на создание задачи, сохраняет ее в базу данных со статусом PENDING
     * и инициирует асинхронное выполнение.
     *
     * @param request DTO с данными для создания задачи.
     * @return Ответ 201 Created с созданной задачей в теле и ссылкой на нее в заголовке Location.
     */
    @PostMapping
    @Operation(summary = "{api.task.create.summary}", description = "{api.task.create.description}")
    @ApiResponse(responseCode = "201", description = "{api.response.created}")
    @ApiResponse(responseCode = "400", description = "{api.response.badrequest}",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskDto createdTask = taskService.createTask(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTask.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdTask);
    }

    /**
     * Получает информацию о задаче по ее ID.
     *
     * @param id Уникальный идентификатор задачи.
     * @return Ответ 200 OK с DTO задачи или 404 Not Found, если задача не найдена.
     */
    @GetMapping("/{id}")
    @Operation(summary = "{api.task.getbyid.summary}", description = "{api.task.getbyid.description}")
    @ApiResponse(responseCode = "200", description = "{api.response.ok}")
    @ApiResponse(responseCode = "404", description = "{api.response.notfound}",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TaskDto> getTaskById(
            @Parameter(description = "{api.task.id.description}") @PathVariable Long id) {
        return taskService.findTaskById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Получает список всех задач с пагинацией.
     *
     * @param pageable Параметры пагинации (page, size, sort).
     * @return Страница (Page) с задачами.
     */
    @GetMapping
    @Operation(summary = "{api.task.getall.summary}", description = "{api.task.getall.description}")
    @ApiResponse(responseCode = "200", description = "{api.response.ok}")
    public ResponseEntity<Page<TaskDto>> getAllTasks(
            @Parameter(description = "{api.pageable.description}") Pageable pageable) {
        Page<TaskDto> tasks = taskService.findAllTasks(pageable);
        return ResponseEntity.ok(tasks);
    }
}
