package org.cloud.automation.api.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloud.automation.api.domain.AuditEvent;
import org.cloud.automation.api.domain.Task;
import org.cloud.automation.api.domain.TaskStatus;
import org.cloud.automation.api.repository.TaskRepository;
import org.cloud.automation.api.service.AuditService;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.waiters.Ec2Waiter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Обработчик для задач типа "create_ec2".
 * <p>
 * Этот класс отвечает за создание нового EC2-инстанса в AWS на основе параметров,
 * переданных в задаче. Он использует AWS SDK for Java 2.x.
 * <p>
 * <b>Требуемые параметры в {@link Task#getParameters()}:</b>
 * <ul>
 *   <li>{@code imageId} (String) - ID AMI (Amazon Machine Image), например, "ami-0c55b159cbfafe1f0".</li>
 *   <li>{@code instanceType} (String) - Тип инстанса, например, "t2.micro".</li>
 *   <li>{@code keyName} (String) - Имя ключевой пары для доступа.</li>
 *   <li>{@code securityGroupIds} (List<String>) - Список ID групп безопасности.</li>
 *   <li>{@code subnetId} (String) - ID подсети, в которой будет запущен инстанс.</li>
 *   <li>{@code minCount} (Integer, опционально, по умолчанию 1) - Минимальное кол-во инстансов.</li>
 *   <li>{@code maxCount} (Integer, опционально, по умолчанию 1) - Максимальное кол-во инстансов.</li>
 * </ul>
 * <p>
 * <b>Формат результата в {@link Task#getResult()}:</b>
 * <ul>
 *   <li>{@code instanceId} (String) - ID созданного EC2-инстанса.</li>
 *   <li>{@code publicIp} (String) - Публичный IP-адрес инстанса.</li>
 *   <li>{@code privateIp} (String) - Приватный IP-адрес инстанса.</li>
 * </ul>
 * <p>
 * <b>Пример использования (клиентский код на Java):</b>
 * <pre>{@code
 * RestTemplate restTemplate = new RestTemplate();
 * String apiUrl = "http://localhost:8080/api/v1/tasks";
 *
 * Map<String, Object> ec2Params = new HashMap<>();
 * ec2Params.put("imageId", "ami-0c55b159cbfafe1f0");
 * ec2Params.put("instanceType", "t2.micro");
 * ec2Params.put("keyName", "my-aws-key");
 * ec2Params.put("securityGroupIds", List.of("sg-12345678"));
 * ec2Params.put("subnetId", "subnet-abcdefgh");
 *
 * CreateTaskRequest request = CreateTaskRequest.builder()
 *         .type("create_ec2")
 *         .parameters(ec2Params)
 *         .build();
 *
 * try {
 *     TaskDto responseDto = restTemplate.postForObject(apiUrl, request, TaskDto.class);
 *     System.out.println("Task created with ID: " + responseDto.getId());
 * } catch (HttpClientErrorException e) {
 *     System.err.println("Error creating task: " + e.getResponseBodyAsString());
 * }
 * }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateEc2Processor implements TaskProcessor {

    private static final String TYPE = "create_ec2";

    private final Ec2Client ec2Client;
    private final TaskRepository taskRepository;
    private final AuditService auditService;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(Task task) {
        log.info("Executing create_ec2 task for Task ID: {}", task.getId());
        Map<String, Object> params = task.getParameters();

        try {
            // 1. Создаем запрос на запуск инстанса
            RunInstancesRequest runRequest = buildRunInstancesRequest(params);

            // 2. Запускаем инстанс
            RunInstancesResponse response = ec2Client.runInstances(runRequest);
            if (!response.hasInstances()) {
                throw Ec2Exception.builder().message("No instances were created in the response.").build();
            }
            String instanceId = response.instances().getFirst().instanceId();
            log.info("Successfully initiated instance creation, instanceId: {}", instanceId);

            // 3. Ждем, пока инстанс перейдет в состояние "running"
            log.info("Waiting for instance {} to be in 'running' state...", instanceId);
            Ec2Waiter waiter = ec2Client.waiter();
            DescribeInstancesRequest waitRequest = DescribeInstancesRequest.builder().instanceIds(instanceId).build();
            waiter.waitUntilInstanceRunning(waitRequest);
            log.info("Instance {} is now running.", instanceId);

            // 4. Получаем финальные детали инстанса (включая IP-адреса)
            DescribeInstancesResponse describeResponse = ec2Client.describeInstances(waitRequest);
            Instance instance = describeResponse.reservations().getFirst().instances().getFirst();

            // 5. Формируем результат и обновляем задачу
            Map<String, Object> result = new HashMap<>();
            result.put("instanceId", instance.instanceId());
            result.put("publicIp", instance.publicIpAddress());
            result.put("privateIp", instance.privateIpAddress());

            task.setResult(result);
            task.setStatus(TaskStatus.SUCCESS);

        } catch (Exception e) {
            log.error("Failed to execute create_ec2 task for Task ID: {}", task.getId(), e);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            task.setCompletedAt(LocalDateTime.now());
            Task savedTask = taskRepository.save(task);

            AuditEvent finalEvent = savedTask.getStatus() == TaskStatus.SUCCESS ? AuditEvent.COMPLETED : AuditEvent.FAILED;
            auditService.logEvent(savedTask, finalEvent, Map.of("finalStatus", savedTask.getStatus().toString()));

            log.info("Task {} completed with status: {}", task.getId(), task.getStatus());
        }
    }

    @SuppressWarnings("unchecked")
    private RunInstancesRequest buildRunInstancesRequest(Map<String, Object> params) {
        return RunInstancesRequest.builder()
                .imageId(getParam(params, "imageId"))
                .instanceType(getParam(params, "instanceType"))
                .keyName(getParam(params, "keyName"))
                .securityGroupIds((List<String>) params.get("securityGroupIds"))
                .subnetId(getParam(params, "subnetId"))
                .minCount(Integer.parseInt(getParam(params, "minCount", "1")))
                .maxCount(Integer.parseInt(getParam(params, "maxCount", "1")))
                .build();
    }

    private String getParam(Map<String, Object> params, String key) {
        return Objects.requireNonNull(params.get(key), "Parameter '" + key + "' is missing.").toString();
    }

    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value == null ? defaultValue : value.toString();
    }
}
