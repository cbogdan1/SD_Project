# Stage 1: build the jar with Maven.
# The build context is the project root, because Maven also needs the parent pom.xml and the common module.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
# -pl = build only this module, -am = also build the modules it depends on (common)
RUN mvn -B -pl services/user_management -am package -DskipTests

# Stage 2: run the jar on a small Java runtime image (no Maven, no source code inside)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /src/services/user_management/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
