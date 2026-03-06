# Этап 1: Сборка приложения
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /app

# Копируем только файлы сборки
COPY build.gradle settings.gradle ./

# Используем кэш Docker BuildKit для зависимостей Gradle.
# Это создает постоянный кэш, который "переживает" сборки.
# Если на вашей машине уже есть кэш, Docker сможет его использовать.
RUN --mount=type=cache,target=/home/gradle/.gradle gradle dependencies --no-daemon

# Копируем исходный код
COPY src src/

# Собираем приложение, используя тот же кэш.
RUN --mount=type=cache,target=/home/gradle/.gradle gradle bootJar --no-daemon -x test

# Этап 2: Создание легковесного образа для запуска
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
