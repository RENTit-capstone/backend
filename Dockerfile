# 1) Build 단계: 테스트 실행 + JAR 패키징
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# 소스 전체 복사
COPY . .

# 1-1) test 프로파일로 테스트 실행
#   옵션은 gradle 명령어 바로 뒤에 와야 합니다.
RUN gradle --no-daemon -Dspring.profiles.active=test test

# 1-2) 테스트 제외하고 패키징
RUN gradle --no-daemon bootJar -x test

# 2) Run 단계: 실제 실행 이미지만 추출
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# fat JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
