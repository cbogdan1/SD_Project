FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# jar built beforehand with `mvn clean package` from the project root
COPY target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
