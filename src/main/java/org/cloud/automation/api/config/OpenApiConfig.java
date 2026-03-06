package org.cloud.automation.api.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class OpenApiConfig {

    private static final Locale RUSSIAN_LOCALE = new Locale("ru");

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(Locale.ENGLISH);
        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    @Bean
    public GroupedOpenApi englishApi(MessageSource messageSource, OperationCustomizer englishOperationCustomizer) {
        return createGroupedApi("en", Locale.ENGLISH, messageSource, englishOperationCustomizer);
    }

    @Bean
    public GroupedOpenApi russianApi(MessageSource messageSource, OperationCustomizer russianOperationCustomizer) {
        return createGroupedApi("ru", RUSSIAN_LOCALE, messageSource, russianOperationCustomizer);
    }

    @Bean
    public OperationCustomizer englishOperationCustomizer(MessageSource messageSource) {
        return createOperationCustomizer(Locale.ENGLISH, messageSource);
    }

    @Bean
    public OperationCustomizer russianOperationCustomizer(MessageSource messageSource) {
        return createOperationCustomizer(RUSSIAN_LOCALE, messageSource);
    }
    /**
     * Helper method to create a localized GroupedOpenApi bean.
     */
    private GroupedOpenApi createGroupedApi(String group, Locale locale, MessageSource messageSource, OperationCustomizer customizer) {
        return GroupedOpenApi.builder()
                .group(group)
                .pathsToMatch("/api/v1/**")
                .addOperationCustomizer(customizer)
                .addOpenApiCustomizer(openApi -> openApi
                        .info(new Info()
                                .title(messageSource.getMessage("api.info.title", null, locale))
                                .version(messageSource.getMessage("api.info.version", null, locale))
                                .description(messageSource.getMessage("api.info.description", null, locale))
                                .termsOfService(messageSource.getMessage("api.info.termsOfService", null, locale))
                                .license(new License()
                                        .name(messageSource.getMessage("api.info.license.name", null, locale))
                                        .url(messageSource.getMessage("api.info.license.url", null, locale)))))
                .build();
    }

    /**
     * Helper method to create a localized OperationCustomizer bean.
     */
    private OperationCustomizer createOperationCustomizer(Locale locale, MessageSource messageSource) {
        return (operation, handlerMethod) -> {
            replaceWithMessage(operation::getSummary, operation::setSummary, messageSource, locale);
            replaceWithMessage(operation::getDescription, operation::setDescription, messageSource, locale);
            if (operation.getResponses() != null) {
                operation.getResponses().forEach((status, apiResponse) ->
                        replaceWithMessage(apiResponse::getDescription, apiResponse::setDescription, messageSource, locale)
                );
            }
            if (operation.getParameters() != null) {
                operation.getParameters().forEach(param ->
                        replaceWithMessage(param::getDescription, param::setDescription, messageSource, locale)
                );
            }
            return operation;
        };
    }

    /**
     * Replaces a string key (like {key}) with a localized message from the message source.
     */
    private void replaceWithMessage(Supplier<String> getter, Consumer<String> setter,
                                    MessageSource messageSource, Locale locale) {
        String value = getter.get();
        if (value != null && value.startsWith("{") && value.endsWith("}")) {
            String code = value.substring(1, value.length() - 1);
            try {
                setter.accept(messageSource.getMessage(code, null, locale));
            } catch (NoSuchMessageException e) {
                log.warn("No message found for code: {} and locale: {}", code, locale);
                setter.accept(code); // Keep the original code as a fallback
            }
        }
    }
}
