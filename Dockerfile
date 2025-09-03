FROM openjdk:17-jdk-slim

WORKDIR /app

# Копируем gradle wrapper и файлы сборки
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Делаем gradlew исполняемым
RUN chmod +x ./gradlew

# Копируем исходный код
COPY src src

# Собираем приложение БЕЗ ТЕСТОВ
RUN ./gradlew build --no-daemon -x test

# Запускаем приложение
EXPOSE 8080
CMD ["./gradlew", "run", "--no-daemon"]