package org.cloud.automation.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

/**
 * Конфигурация для клиентов AWS SDK.
 * <p>
 * Этот класс отвечает за создание и настройку бинов клиентов для взаимодействия с сервисами AWS.
 */
@Configuration
public class AwsConfig {

    /**
     * Регион AWS, который будет использоваться клиентами.
     * Значение внедряется из `application.properties` (ключ: `aws.region`).
     */
    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    /**
     * Создает и конфигурирует клиент для работы с AWS EC2.
     * <p>
     * Клиент использует {@link DefaultCredentialsProvider}, который автоматически ищет учетные данные
     * в стандартных местах (переменные окружения, системные свойства, файл ~/.aws/credentials, IAM-роль инстанса).
     * Это делает приложение гибким для запуска как локально, так и в облачной среде AWS.
     *
     * @return Сконфигурированный клиент EC2.
     */
    @Bean
    @Profile("!test")
    public Ec2Client ec2Client() {
        return Ec2Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
