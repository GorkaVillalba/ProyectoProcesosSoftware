# ═══════════════════════════════════════════════════════════════
# T-02 (Persona 1): Dockerfile multi-stage
# CREAR en la raíz del proyecto: Dockerfile
# ═══════════════════════════════════════════════════════════════

# Stage 1: Build
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle build -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
