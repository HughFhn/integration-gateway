# Use official OpenJDK 17 slim image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy built JAR into container
COPY target/gateway-0.0.1-SNAPSHOT.jar app.jar

# Expose Spring Boot port
EXPOSE 8081

# Run the JAR
ENTRYPOINT ["java","-jar","app.jar"]
