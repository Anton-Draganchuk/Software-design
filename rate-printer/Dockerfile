FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY currency-rate-provider/pom.xml currency-rate-provider/pom.xml
COPY rate-printer/pom.xml rate-printer/pom.xml
RUN mvn -pl rate-printer -am dependency:go-offline

COPY rate-printer/src rate-printer/src
RUN mvn -pl rate-printer -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/rate-printer/target/rate-printer-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
