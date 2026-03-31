FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /build
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Asia/Seoul
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
