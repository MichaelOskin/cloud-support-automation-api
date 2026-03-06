package org.cloud.automation.api.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.cloud.automation.api.domain.AuditEvent;
import org.cloud.automation.api.domain.Task;
import org.cloud.automation.api.domain.TaskStatus;
import org.cloud.automation.api.domain.User;
import org.cloud.automation.api.dto.CreateTaskRequest;
import org.cloud.automation.api.dto.TaskDto;
import org.cloud.automation.api.processor.TaskProcessor;
import org.cloud.automation.api.repository.TaskRepository;
import org.cloud.automation.api.repository.UserRepository;
import org.cloud.automation.api.service.AuditService;
import org.cloud.automation.api.service.TaskProcessorRegistry;
import org.cloud.automation.api.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskProcessorRegistry processorRegistry;
    private final AuditService auditService;
    private final TaskService self; // Self-injection to allow @Async calls from within the same class

    @Autowired
    public TaskServiceImpl(TaskRepository taskRepository, UserRepository userRepository, TaskProcessorRegistry processorRegistry, AuditService auditService, @Lazy TaskService self) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.processorRegistry = processorRegistry;
        this.auditService = auditService;
        this.self = self;
    }

    @Override
    @Transactional
    public TaskDto createTask(CreateTaskRequest request) {
        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.getUserId()));
        }

        Task task = Task.builder()
                .type(request.getType())
                .parameters(request.getParameters())
                .user(user)
                .status(TaskStatus.PENDING)
                .build();

        Task savedTask = taskRepository.save(task);
        auditService.logEvent(savedTask, AuditEvent.CREATED, null);

        // Trigger asynchronous task execution ONLY after the current transaction commits
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                self.processTask(savedTask.getId());
            }
        });

        return toDto(savedTask);
    }

    @Override
    @Async("taskExecutor")
    @Transactional
    public void processTask(Long taskId) {
        log.info("Starting to process task with ID: {}", taskId);
        Optional<Task> taskOpt = taskRepository.findById(taskId);

        if (taskOpt.isEmpty()) {
            log.error("Task with ID {} not found for processing.", taskId);
            return;
        }

        Task task = taskOpt.get();
        Optional<TaskProcessor> processorOpt = processorRegistry.getProcessor(task.getType());

        if (processorOpt.isEmpty()) {
            log.error("No processor found for task type: {}. Marking task as FAILED.", task.getType());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("Unsupported task type: " + task.getType());
            task.setCompletedAt(LocalDateTime.now());
            Task failedTask = taskRepository.save(task);
            auditService.logEvent(failedTask, AuditEvent.FAILED, Map.of("reason", "Unsupported task type"));
            return;
        }

        try {
            task.setStatus(TaskStatus.RUNNING);
            Task runningTask = taskRepository.save(task);
            auditService.logEvent(runningTask, AuditEvent.STARTED, null);
            log.info("Task {} is now RUNNING.", taskId);

            processorOpt.get().execute(runningTask); // The processor is responsible for the final status update

        } catch (Exception e) {
            log.error("Exception during processing task {}: {}", taskId, e.getMessage(), e);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            Task failedTask = taskRepository.save(task);
            auditService.logEvent(failedTask, AuditEvent.FAILED, Map.of("reason", e.getMessage()));
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<TaskDto> findTaskById(Long id) {
        return taskRepository.findById(id).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskDto> findAllTasks(Pageable pageable) {
        return taskRepository.findAll(pageable).map(this::toDto);
    }

    private TaskDto toDto(Task task) {
        return TaskDto.builder()
                .id(task.getId())
                .type(task.getType())
                .status(task.getStatus())
                .parameters(task.getParameters())
                .result(task.getResult())
                .errorMessage(task.getErrorMessage())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .completedAt(task.getCompletedAt())
                .userId(task.getUser() != null ? task.getUser().getId() : null)
                .build();
    }
}
