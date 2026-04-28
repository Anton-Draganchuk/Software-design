FROM eclipse-temurin:17-jre
ARG APP_JAR=currency-rate-provider/target/currency-rate-provider-1.0.0.jar
WORKDIR /app
COPY ${APP_JAR} app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
