package org.cloud.automation.api.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Представляет пользователя системы автоматизации.
 * <p>
 * В контексте данного API, пользователь — это субъект, который инициирует выполнение задач ({@link Task}).
 * Наличие этой сущности позволяет вести аудит действий (кто какую задачу создал) и является основой
 * для будущего расширения системы функциями аутентификации и авторизации (например, через API-ключи или JWT).
 *
 * @see Task
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    /**
     * Уникальный идентификатор пользователя (Primary Key).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Имя пользователя.
     */
    @NotBlank(message = "Name cannot be blank")
    @Column(nullable = false)
    private String name;

    /**
     * Электронная почта пользователя. Используется как уникальный логин в системе.
     */
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email cannot be blank")
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Роль пользователя в системе. В будущем может использоваться для разграничения прав доступа
     * (например, разрешать определенным ролям создавать только определенные типы задач).
     *
     * @see UserRole
     */
    @NotNull(message = "Role cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

}
