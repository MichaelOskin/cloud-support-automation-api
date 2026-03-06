package org.cloud.automation.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Конфигурация для асинхронного выполнения задач в Spring.
 * <p>
 * Эта конфигурация активирует поддержку {@code @Async} и определяет кастомный пул потоков
 * для выполнения фоновых задач. Параметры пула вынесены в application.properties
 * и могут быть легко изменены для разных окружений.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${spring.task.execution.pool.core-size:5}")
    private int corePoolSize;

    @Value("${spring.task.execution.pool.max-size:10}")
    private int maxPoolSize;

    @Value("${spring.task.execution.pool.queue-capacity:25}")
    private int queueCapacity;

    @Value("${spring.task.execution.thread-name-prefix:AsyncTask-}")
    private String threadNamePrefix;


    /**
     * Определяет бин исполнителя задач (Executor), который будет использоваться для всех методов,
     * помеченных аннотацией {@code @Async("taskExecutor")}.
     *
     * @return Конфигурированный пул потоков.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.initialize();
        return executor;
    }
}
