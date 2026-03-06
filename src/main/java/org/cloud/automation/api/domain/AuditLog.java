package org.cloud.automation.api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Сущность для записи событий аудита, связанных с жизненным циклом {@link Task}.
 * Каждая запись представляет собой одно событие (например, создание, запуск, завершение).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_log")
public class AuditLog {

    /**
     * Уникальный идентификатор записи аудита.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Задача, с которой связано это событие аудита.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /**
     * Тип произошедшего события.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditEvent event;

    /**
     * Дополнительные детали события в формате JSONB.
     * Например, может содержать информацию о том, какой пользователь инициировал событие.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    /**
     * Временная метка события. Устанавливается автоматически.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
