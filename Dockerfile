# Imagine de baza cu Java 17
FROM eclipse-temurin:17-jdk-alpine

# Directorul de lucru in interiorul containerului
WORKDIR /app

# Copiaza fisierul .jar generat de Maven in container
COPY target/*.jar app.jar

# Expune portul aplicatiei
EXPOSE 8080

# Comanda de start
ENTRYPOINT ["java", "-jar", "app.jar"]