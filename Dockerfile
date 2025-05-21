# 1) Build 단계: 테스트 실행 + JAR 패키징
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# 소스 전체 복사
COPY . .

# production 설정
COPY src/main/resources/application.yml            src/main/resources/application.yml
# test 전용 설정
COPY src/main/resources/application-test.yml       src/main/resources/application-test.yml

# 1-1) test 프로파일로 테스트 실행 (application-test.yml 적용)
RUN gradle test --no-daemon -Dspring.profiles.active=test

# 1-2) 테스트 제외하고 패키징
RUN gradle bootJar --no-daemon -x test

# 2) Run 단계: 실제 실행 이미지만 추출
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# fat JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
