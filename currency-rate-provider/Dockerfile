FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY currency-rate-provider/pom.xml currency-rate-provider/pom.xml
COPY rate-printer/pom.xml rate-printer/pom.xml
RUN mvn -pl currency-rate-provider -am dependency:go-offline

COPY currency-rate-provider/src currency-rate-provider/src
RUN mvn -pl currency-rate-provider -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/currency-rate-provider/target/currency-rate-provider-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
