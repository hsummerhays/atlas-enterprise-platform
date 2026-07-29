# Stage 1: Build the application using Gradle
FROM eclipse-temurin:25-jdk-jammy AS builder
WORKDIR /app

# Copy gradle files
COPY gradle /app/gradle
COPY gradlew /app/gradlew
COPY settings.gradle /app/settings.gradle
COPY build.gradle /app/build.gradle

# atlas-agent-platform/build.gradle must exist so Gradle can evaluate settings.gradle's
# included projects, even though this image only builds atlas-shipping-app.
COPY atlas-agent-platform/build.gradle /app/atlas-agent-platform/build.gradle
COPY atlas-shipping-app /app/atlas-shipping-app

# Build the shipping application only
RUN ./gradlew :atlas-shipping-app:bootJar --no-daemon -x test

# Stage 2: Runtime image
FROM eclipse-temurin:25-jdk-jammy

# Run as a fixed non-root UID/GID matching the Kubernetes securityContext
# (runAsUser/runAsGroup: 1000 in k8s/deployment.yaml) so the image is secure by
# default even when run outside Kubernetes (e.g. plain `docker run`).
RUN addgroup --system --gid 1000 appgroup \
    && adduser --system --uid 1000 --gid 1000 --no-create-home --shell /usr/sbin/nologin appuser

WORKDIR /app
COPY --from=builder --chown=appuser:appgroup /app/atlas-shipping-app/build/libs/*.jar app.jar

USER 1000:1000

# Expose port
EXPOSE 8080

# Run with virtual threads configuration enabled if needed
ENTRYPOINT ["java", "-jar", "app.jar"]
