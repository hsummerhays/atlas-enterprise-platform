# Stage 1: Build the application using Gradle
FROM eclipse-temurin:25-jdk-jammy AS builder
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
FROM eclipse-temurin:25-jdk-jammy

# Run as a fixed non-root UID/GID matching the Kubernetes securityContext
# (runAsUser/runAsGroup: 1000 in k8s/deployment.yaml) so the image is secure by
# default even when run outside Kubernetes (e.g. plain `docker run`).
RUN addgroup --system --gid 1000 appgroup \
    && adduser --system --uid 1000 --gid 1000 --no-create-home --shell /usr/sbin/nologin appuser

WORKDIR /app
COPY --from=builder --chown=appuser:appgroup /app/build/libs/*.jar app.jar

USER 1000:1000

# Expose port
EXPOSE 8080

# Run with virtual threads configuration enabled if needed
ENTRYPOINT ["java", "-jar", "app.jar"]
