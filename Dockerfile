FROM gradle:8.5-jdk21 AS builder

# Base64 인코딩된 시크릿
ARG APPLICATION_YML
ARG FCM_JSON

WORKDIR /app

# gradle 캐시 활용을 위해 의존성 파일 먼저 복사
COPY settings.gradle build.gradle gradle ./
COPY gradlew .
RUN chmod +x gradlew

# 의존성만 받아 두면 이후 레이어 캐시 효율 ↑
RUN ./gradlew --no-daemon dependencyResolutionManagement

# 실제 소스 복사
COPY src ./src

# 리소스 주입 (Base64 → 평문)
RUN mkdir -p src/main/resources/firebase \
 && echo "$APPLICATION_YML" | base64 -d > src/main/resources/application.yml \
 && echo "$FCM_JSON"        | base64 -d > src/main/resources/firebase/fcm.json

# 테스트 + 패키징 (테스트는 한 번만 실행)
RUN ./gradlew --no-daemon clean test \
 && ./gradlew --no-daemon bootJar -x asciidoctor -x test

# │ 2) Run 단계: 실제 실행 이미지만 추출
FROM eclipse-temurin:21-jre-alpine

ENV TZ=Asia/Seoul \
    LOG_PATH=/var/log/spring

# Alpine 필수 유틸
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    mkdir -p $LOG_PATH

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ARG FCM_JSON
ENV FCM_JSON="${FCM_JSON}"

ENTRYPOINT ["java","-Duser.timezone=Asia/Seoul","-jar","app.jar"]
