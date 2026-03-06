package org.cloud.automation.api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Основная сущность системы, представляющая асинхронную задачу на выполнение.
 * <p>
 * В соответствии с ТЗ, объект Task является центральной единицей работы в API. Он создается в ответ на POST-запрос
 * и проходит жизненный цикл: {@link TaskStatus#PENDING} -> {@link TaskStatus#RUNNING} -> {@link TaskStatus#SUCCESS}
 * или {@link TaskStatus#FAILED}.
 * <p>
 * Эта сущность спроектирована как полиморфная: поле {@code type} определяет конкретный вид задачи
 * (например, "create_ec2", "ssh_command"), а поля {@code parameters} и {@code result} хранят специфичные для этого типа
 * данные в формате JSONB. Такой подход позволяет легко расширять систему новыми типами задач без изменения схемы БД.
 *
 * @see org.cloud.automation.api.service.TaskService
 * @see org.cloud.automation.api.controller.TaskController
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tasks")
public class Task {

    /**
     * Уникальный идентификатор задачи (Primary Key). Генерируется автоматически.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Тип задачи. Это текстовый дискриминатор, который определяет, какой обработчик (TaskProcessor) будет выполнять задачу.
     * Например: "create_ec2", "ssh_command". Новые типы могут быть добавлены без изменения модели.
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * Параметры для выполнения задачи, хранящиеся в формате JSONB.
     * Структура этого поля полностью зависит от {@link #type}.
     * <p>
     * <b>Пример для type="create_ec2":</b><br>
     * {@code {"imageId": "ami-123", "instanceType": "t2.micro"}}
     * <p>
     * <b>Пример для type="ssh_command":</b><br>
     * {@code {"host": "1.2.3.4", "command": "df -h"}}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> parameters;

    /**
     * Текущий статус выполнения задачи. Определяет этап в жизненном цикле задачи.
     *
     * @see TaskStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    /**
     * Результат успешного выполнения задачи, хранимый в формате JSONB.
     * Структура этого поля зависит от {@link #type}. Поле равно {@code null}, если задача не была успешной.
     * <p>
     * <b>Пример для type="create_ec2":</b><br>
     * {@code {"instanceId": "i-12345", "publicIp": "54.1.2.3"}}
     * <p>
     * <b>Пример для type="ssh_command":</b><br>
     * {@code {"exitCode": 0, "stdout": "...", "stderr": ""}}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> result;

    /**
     * Сообщение об ошибке, если задача завершилась со статусом {@link TaskStatus#FAILED}.
     * В случае успеха поле равно {@code null}.
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Временная метка создания задачи. Устанавливается автоматически при первом сохранении.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Временная метка последнего обновления задачи. Обновляется автоматически при каждом изменении.
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Временная метка завершения задачи (как успешного, так и неуспешного).
     * Устанавливается вручную в коде, когда задача переходит в финальный статус.
     */
    private LocalDateTime completedAt;

    /**
     * Ссылка на пользователя, который инициировал задачу.
     * Используется для аудита и в будущем может использоваться для разграничения прав доступа.
     * Поле не является обязательным.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
