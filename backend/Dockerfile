# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S spukhaus && adduser -S spukhaus -G spukhaus
COPY --from=build /build/target/backend-*.jar app.jar
RUN chown spukhaus:spukhaus app.jar
USER spukhaus

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
