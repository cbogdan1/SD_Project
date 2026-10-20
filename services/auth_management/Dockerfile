FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copiaza fisierul .jar generat de Maven in container
COPY target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
