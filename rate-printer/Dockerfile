FROM eclipse-temurin:17-jre
ARG APP_JAR=rate-printer/target/rate-printer-1.0.0.jar
WORKDIR /app
COPY ${APP_JAR} app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
