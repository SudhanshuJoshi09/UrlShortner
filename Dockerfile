# Use an official OpenJDK runtime as a base image
FROM openjdk:17-oracle

# Set the working directory in the container
WORKDIR /app

COPY src/main/resources/application.properties /app/
COPY target/*.jar /app/app.jar

CMD ["java", "-jar", "/app/app.jar"]