# 1. Build with Gradle
# Original Image
FROM gradle:8.5-jdk21 AS builder
# Dir
WORKDIR /app
# Copy files into container
COPY . .
COPY src/main/resources/application.yml ./application.yml
# Run gradle to build
RUN gradle bootJar --no-daemon

# 2. Run Spring
# Original Image
FROM eclipse-temurin:21-jre-alpine
# Dir
WORKDIR /app
# Copy files into container
COPY --from=builder /app/build/libs/*.jar app.jar
# Exposing port
EXPOSE 8080
# Run Spring
CMD ["java", "-jar", "app.jar"]
