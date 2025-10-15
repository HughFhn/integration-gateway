# Stage 1: Build the project with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set working directory inside the container
WORKDIR /app

# Copy pom.xml and download dependencies first (to cache them)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy all source files
COPY src ./src

# Build the project and package it
RUN mvn clean package spring-boot:repackage -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:20-jre

WORKDIR /app

# Copy the jar from the build stage
COPY --from=build /app/target/gateway-0.0.1-SNAPSHOT.jar ./gateway.jar

# Expose the port your app runs on
EXPOSE 8081

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "gateway.jar"]
