# maven jdk-17 image
FROM maven:3.8.3-openjdk-17
WORKDIR /app

COPY src/ /app/src
COPY pom.xml /app/pom.xml
COPY mvnw /app/mvnw

RUN mvn package
CMD ["mvn", "spring-boot:run"]