# История разработки - Итерация 1

## Цель
Реализация базовой архитектуры и CRUD-функциональности для основных сущностей REST API в соответствии с техническим заданием.

## Проделанная работа

### 1. Проектирование
- Создана **PlantUML диаграмма классов** (`plantuml/class_diagram.puml`) для визуализации модели данных и отношений между `User`, `Ticket`, и `KnowledgeBaseArticle`.

### 2. Слой данных (Domain & Repository)
- **`User`**:
    - Создана JPA-сущность `User` и перечисление `UserRole`.
    - Создан `UserRepository` (Spring Data JPA) для доступа к данным пользователей.
- **`Ticket`**:
    - Создана JPA-сущность `Ticket` и перечисление `TicketStatus`.
    - Реализованы отношения `Many-to-One` с `User` (creator, assignee).
    - Создан `TicketRepository`.
- **`KnowledgeBaseArticle`**:
    - Создана JPA-сущность `KnowledgeBaseArticle`.
    - Настроена двунаправленная связь `Many-to-Many` с `Ticket`.
    - Создан `KnowledgeBaseArticleRepository` с методом для поиска по заголовку.

### 3. Слой DTO (Data Transfer Objects)
- Для каждой сущности созданы DTO для передачи данных (`UserDto`, `TicketDto`, `KnowledgeBaseArticleDto`).
- Созданы отдельные DTO для запросов на создание (`CreateUserRequest`, `CreateTicketRequest`, `CreateKnowledgeBaseArticleRequest`) для чистоты API.

### 4. Сервисный слой (Business Logic)
- **`UserService`**: Реализована логика для CRUD-операций с пользователями.
- **`TicketService`**: Реализована логика для CRUD-операций с тикетами, включая обработку связей с `User`.
- **`KnowledgeBaseArticleService`**: Реализована логика для CRUD-операций и поиска статей.
- Для всех сервисов использован паттерн "интерфейс + реализация" (`/service` и `/service/impl`).

### 5. Слой контроллеров (API Endpoints)
- **`UserController`**: Реализованы REST-эндпоинты для полного CRUD-цикла управления пользователями.
- **`TicketController`**: Реализованы REST-эндпоинты для CRUD-операций с тикетами.
- **`KnowledgeBaseArticleController`**: Реализованы REST-эндпоинты для CRUD и поиска статей.
- Все контроллеры используют `ResponseEntity` для корректных HTTP-ответов и кодов состояния.

## Итог
Заложена архитектурная основа приложения. Реализован полный вертикальный срез функциональности для всех ключевых сущностей, что позволяет перейти к разработке основной бизнес-логики.

# История разработки - Итерация 2

## Цель
Настройка подключения к базе данных, запуск и верификация работоспособности приложения.

## Проделанная работа

### 1. Настройка Базы Данных
- **Обсуждение и выбор стратегии**: Принято решение использовать PostgreSQL как для разработки, так и для продакшена для максимальной консистентности окружений.
- **Реализация через Spring Profiles**:
    - В `application.properties` установлен профиль по умолчанию `dev`.
    - Создан `application-dev.properties` для автоматического запуска PostgreSQL в Docker-контейнере с помощью **Testcontainers**. Это упрощает локальную разработку, так как не требует ручной установки и настройки базы данных.
    - Создан `application-prod.properties` с плейсхолдерами и рекомендациями для настройки подключения к production-базе данных.

### 2. Устранение неисправностей (Troubleshooting)
- **Диагностика**: При первом запуске возникла ошибка `ClassNotFoundException` для драйвера Testcontainers.
- **Решение**: Проблема была в неправильной области видимости (`scope`) зависимости `spring-boot-testcontainers` в `build.gradle`. Она была в `testImplementation` (только для тестов) вместо `developmentOnly` (для запуска приложения в режиме разработки).
- **Исправление**: Файл `build.gradle` был скорректирован для перемещения `spring-boot-testcontainers` и `org.testcontainers:postgresql` в `developmentOnly`.

### 3. Верификация
- **Успешный запуск**: После исправления зависимостей приложение было успешно запущено.
- **Тестирование API**: С помощью `curl`-запросов был протестирован полный CRUD-цикл для эндпоинта `/api/v1/users`:
    - `POST` (создание) -> `201 Created`
    - `GET` (получение по ID) -> `200 OK`
    - `DELETE` (удаление) -> `204 No Content`
    - `GET` (получение всех, проверка удаления) -> `200 OK` и пустой список.

## Итог
Настройка базы данных завершена. Работоспособность приложения и основного API полностью подтверждена. Проект готов к дальнейшему развитию или развертыванию.

# История разработки - Итерация 3

## Цель
Повышение надежности API за счет улучшения обработки ошибок, внедрения продвинутой логики поиска с Hibernate Search и расширения тестового покрытия.

## Проделанная работа

### 1. Улучшенная Обработка Ошибок
- Создан структурированный **`ErrorResponse` DTO** для унифицированных ответов об ошибках.
- Реализовано кастомное исключение **`ResourceNotFoundException`** для случаев, когда запрашиваемый ресурс не найден.
- Внедрен **`GlobalExceptionHandler`** (`@ControllerAdvice`) для централизованной обработки исключений:
    - `ResourceNotFoundException` -> `404 Not Found`
    - `MethodArgumentNotValidException` (ошибки валидации) -> `400 Bad Request` с детальным описанием ошибок.
    - `DataIntegrityViolationException` (нарушения целостности данных, например, дубликаты) -> `409 Conflict`. Улучшен парсинг сообщения об ошибке для более дружелюбных ответов (например, "Email address already exists.").
    - Общий `Exception` -> `500 Internal Server Error` как запасной вариант.
- Рефакторинг сервисного слоя и контроллеров для использования `ResourceNotFoundException` и упрощения логики обработки `Optional`.

### 2. Интеграция Улучшенного Поиска (Hibernate Search)
- **Добавлены зависимости:** `hibernate-search-mapper-orm` и `hibernate-search-backend-lucene` в `build.gradle`.
- **Настроена конфигурация:** В `application-dev.properties` и `application-prod.properties` добавлены настройки для Lucene backend и стратегии управления схемой индекса.
- **Аннотирована сущность:** `KnowledgeBaseArticle` размечена аннотациями `@Indexed`, `@FullTextField` и `@KeywordField` для индексации и полнотекстового поиска.
- **Создан компонент индексации:** `SearchIndexBuilder` для автоматической массовой индексации существующих данных при старте приложения.
- **Обновлен `TicketService`:** Метод `suggestArticlesForTicket` переписан для использования мощного API Hibernate Search, что обеспечивает более релевантные и ранжированные результаты поиска.

### 3. Расширенное Тестирование (Интеграционные Тесты)
- **`KnowledgeBaseArticleControllerIntegrationTest`**: Переписан для использования `TestRestTemplate` и расширен для полного покрытия CRUD-операций, включая поиск статей и обработку ошибок (404, 400). Исправлена проблема с `massIndexer`.
- **`TicketControllerIntegrationTest`**: Дополнен для полного покрытия CRUD-операций с тикетами, валидации, обработки исполнителей и тестирования эндпоинта `suggestArticles`. Исправлена проблема с `massIndexer` и конвертация в `TestRestTemplate`.
- **`UserControllerIntegrationTest`**: Подтверждена работоспособность тестов CRUD и валидации.
- **Устранение ошибок в тестах**:
    - Исправлена ошибка `No transactional EntityManager available` путем оборачивания вызова `massIndexer().startAndWait()` в `transactionTemplate.execute()` внутри `setUp` методов интеграционных тестов.
    - Исправлена ошибка `cannot find symbol: class ResponseEntity` путем добавления соответствующего импорта.
    - Исправлена ошибка `testCreateUser_DuplicateEmail()` путем уточнения сообщения об ошибке для `DataIntegrityViolationException`.

## Итог
API значительно повысил свою надежность и функциональность. Внедрен продвинутый механизм полнотекстового поиска. Существенно расширено покрытие автоматизированными интеграционными тестами, что обеспечивает уверенность в корректной работе всех ключевых функций.

# История разработки - Итерация 4

## Цель
Реализация интернационализации (i18n) для документации Swagger UI (Springdoc-openapi).

## Проделанная работа

### 1. Настройка инфраструктуры i18n
- **`OpenApiConfig.java` рефакторинг:**
    - Реализованы бины `MessageSource` и `LocaleResolver`.
    - Заменен единый бин `OpenAPI` на два `GroupedOpenApi` бина (`englishApi`, `russianApi`), каждый из которых явно устанавливает глобальную локаль для информации API (`Info`). Это создало отдельные эндпоинты Swagger UI: `http://localhost:8080/swagger-ui.html?group=en` и `http://localhost:8080/swagger-ui.html?group=ru`.
    - Вместо единого `OperationCustomizer` созданы два отдельных (`englishOperationCustomizer` и `russianOperationCustomizer`), каждый жестко привязан к своей локали (Locale.ENGLISH или `new Locale("ru")`) и передается в соответствующий бин `GroupedOpenApi`. Это обеспечивает правильную локализацию описаний операций.
- **Созданы файлы сообщений:** `messages.properties` (английский по умолчанию) и `messages_ru.properties` (русский) в `src/main/resources`.
- **Конфигурация Springdoc:** В `application.properties` добавлено `springdoc.api-docs.cache.disabled=true` для отключения кэширования документации и обеспечения динамического переключения языка. Свойство `springdoc.disable-i18n=true` было удалено.

### 2. Рефакторинг аннотаций контроллеров
- Все `@Operation`, `@ApiResponses`, и `@Parameter` аннотации в `UserController`, `TicketController`, и `KnowledgeBaseArticleController` были изменены для использования плейсхолдеров `{key}`.
- Аннотации `@Tag` не поддерживают интернационализацию в Springdoc, поэтому их `name` и `description` были возвращены к жестко закодированным английским строкам.

### 3. Устранение неисправностей
- **"NoUniqueBeanDefinitionException" для `OperationCustomizer`:** Устранена явным указанием имени кастомайзера при инъекции в `GroupedOpenApi` бины.
- **"Ambiguous method call" для `replaceWithMessage`:** Исправлена удалением дублирующего метода `replaceWithMessage`.
- **Некорректная локаль в `OperationCustomizer`:** Решена путем создания двух отдельных, локализованных `OperationCustomizer`, каждый со своей фиксированной локалью.

## Итог
Успешно реализована полная интернационализация документации Swagger UI. Глобальная информация API и описания операций теперь отображаются на выбранном языке через отдельные группы API или динамически (для описаний операций) по заголовку `Accept-Language`.
