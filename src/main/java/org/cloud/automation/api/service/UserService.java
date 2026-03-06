package org.cloud.automation.api.service;

import org.cloud.automation.api.domain.User;
import org.cloud.automation.api.dto.UserDto;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления пользователями ({@link User}).
 * <p>
 * Предоставляет набор стандартных CRUD-операций для работы с сущностями пользователей.
 */
public interface UserService {

    /**
     * Находит пользователя по его уникальному идентификатору.
     * @param id ID пользователя.
     * @return {@link Optional}, содержащий DTO пользователя, если он найден.
     */
    Optional<UserDto> findUserById(Long id);

    /**
     * Возвращает список всех пользователей в системе.
     * @return Список DTO всех пользователей.
     */
    List<UserDto> findAllUsers();

    /**
     * Создает нового пользователя.
     * @param userDto DTO с данными для нового пользователя.
     * @return DTO созданного пользователя с присвоенным ID.
     */
    UserDto createUser(UserDto userDto);

    /**
     * Обновляет данные существующего пользователя.
     * @param id ID пользователя для обновления.
     * @param userDto DTO с новыми данными.
     * @return {@link Optional}, содержащий обновленный DTO пользователя, если пользователь был найден и обновлен.
     */
    Optional<UserDto> updateUser(Long id, UserDto userDto);

    /**
     * Удаляет пользователя по его ID.
     * <p>
     * Операция является идемпотентной: если пользователь с данным ID не существует,
     * никаких действий не производится и ошибка не выбрасывается.
     * @param id ID пользователя для удаления.
     */
    void deleteUser(Long id);

}
