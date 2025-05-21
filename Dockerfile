# │ 1) Build 단계: 테스트 실행 + JAR 패키징
FROM gradle:8.5-jdk21 AS builder

# Build args 로 Base64 인코딩된 시크릿을 넘겨받습니다
ARG APPLICATION_YML
ARG FCM_JSON

WORKDIR /app

# 1-0) 소스 복사
COPY . .

# 1-1) resources 덮어쓰기: Base64 → 디코딩 → 파일로 저장
# 1) 리소스 덮어쓰기
RUN mkdir -p src/main/resources/firebase \
 && echo "$APPLICATION_YML" | base64 --decode > src/main/resources/application.yml \
 && echo "$FCM_JSON"    | base64 --decode > src/main/resources/firebase/rentit-5b36b-firebase-adminsdk-fbsvc-ab4f4216ef.json

# 2) 항상 clean 붙여서 테스트·패키징
RUN gradle --no-daemon clean \
       -Dspring.profiles.active=test test \
 && gradle --no-daemon clean bootJar -x test -x asciidoctor

# 1-2) test 프로파일로 테스트 실행
RUN gradle --no-daemon -Dspring.profiles.active=test test

# 1-3) 테스트 제외하고 패키징
RUN gradle --no-daemon bootJar -x test

# │ 2) Run 단계: 실제 실행 이미지만 추출
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# fat JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ARG FCM_JSON
ENV FCM_JSON="${FCM_JSON}"

ENTRYPOINT ["java","-jar","app.jar"]
