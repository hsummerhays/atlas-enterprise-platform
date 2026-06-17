# Stage 1: Build the application using Gradle
FROM openjdk:25-jdk-slim AS builder
WORKDIR /app

# Copy gradle files
COPY gradle /app/gradle
COPY gradlew /app/gradlew
COPY settings.gradle /app/settings.gradle
COPY build.gradle /app/build.gradle

# Copy source code
COPY src /app/src

# Build the application
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime image
FROM openjdk:25-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Run with virtual threads configuration enabled if needed
ENTRYPOINT ["java", "-jar", "app.jar"]
