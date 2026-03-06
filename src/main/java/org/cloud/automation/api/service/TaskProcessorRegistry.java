package org.cloud.automation.api.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.cloud.automation.api.processor.TaskProcessor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Реестр для хранения и доступа ко всем обработчикам задач (TaskProcessor).
 * <p>
 * Этот сервис при запуске приложения автоматически находит все бины, реализующие интерфейс {@link TaskProcessor},
 * и сохраняет их в карту для быстрого доступа по типу задачи.
 * <p>
 * Такой подход реализует паттерн "Registry" или "Strategy" и позволяет системе быть легко расширяемой.
 * Для добавления нового типа задач достаточно создать новый класс, реализующий {@link TaskProcessor},
 * и пометить его как {@code @Component}. Реестр подхватит его автоматически.
 */
@Service
@RequiredArgsConstructor
public class TaskProcessorRegistry {

    /**
     * Spring автоматически внедрит сюда список всех бинов, реализующих TaskProcessor.
     */
    private final List<TaskProcessor> processors;

    /**
     * Карта для быстрого доступа к процессору по его типу.
     * Ключ - тип задачи (например, "create_ec2"), значение - сам процессор.
     */
    private Map<String, TaskProcessor> processorMap;

    /**
     * Метод, выполняемый после внедрения всех зависимостей.
     * Он инициализирует карту {@code processorMap}, преобразуя список процессоров
     * в карту, где ключом является тип, возвращаемый методом {@link TaskProcessor#getType()}.
     */
    @PostConstruct
    private void init() {
        processorMap = processors.stream()
                .collect(Collectors.toMap(TaskProcessor::getType, Function.identity()));
    }

    /**
     * Находит и возвращает обработчик для указанного типа задачи.
     *
     * @param type Тип задачи (например, "create_ec2").
     * @return {@link Optional}, содержащий найденный процессор, или пустой, если процессор для данного типа не найден.
     */
    public Optional<TaskProcessor> getProcessor(String type) {
        return Optional.ofNullable(processorMap.get(type));
    }
}
