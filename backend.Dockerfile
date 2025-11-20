# Stage 1: Build the application
FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/main ./src/main
RUN mvn clean package -DskipTests spring-boot:repackage

# Stage 2: Create the final image
FROM eclipse-temurin:17-jre-focal
WORKDIR /app
COPY --from=build /app/target/gateway-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
