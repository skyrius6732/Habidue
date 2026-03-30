# 1. 빌드 스테이지 (Gradle 빌드)
FROM gradle:7.6-jdk17 AS build
WORKDIR /home/gradle/project

# Gradle 캐시 활용을 위해 설정 파일 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 의존성 먼저 다운로드 (실패해도 무시하고 빌드 시도)
RUN ./gradlew build -x test --no-daemon || return 0

# 전체 소스 복사 및 빌드
COPY . .
RUN ./gradlew clean bootJar --no-daemon -x test

# 2. 실행 스테이지 (최종 이미지)
FROM openjdk:17-jdk-slim
WORKDIR /app

# 타임존 설정 (한국 시간)
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 빌드 스테이지에서 생성된 jar 복사
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

# 운영 환경 포트 오픈
EXPOSE 8080

# 실행 명령 (운영 프로파일 적용)
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
