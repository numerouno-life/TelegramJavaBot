# Используем официальный образ Java 21 (JRE-only для уменьшения размера)
FROM eclipse-temurin:21-jre-jammy

# Указываем рабочую директорию
WORKDIR /app

# Копируем JAR-файл (лучше явно указать имя)
COPY target/javabot-*.jar app.jar

# Оптимизированные параметры запуска
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]