# ---- Build Stage ----
# Use Maven base image from the Docker Hub
FROM maven:3.8.3-openjdk-11-slim AS build

# Set the current working directory inside the image
WORKDIR /app

# Copy the source code to the container
COPY src /app/src
COPY pom.xml /app

# Zadanie 1: uruchom testy jednostkowe PRZED zbudowaniem obrazu.
# Faza 'verify' wykonuje faze 'test' (Surefire). Jesli jakikolwiek test
# zakonczy sie niepowodzeniem, Maven zwraca kod != 0, a 'docker build'
# przerywa sie bledem i obraz NIE powstaje.
# (Poprzednio bylo: 'mvn clean install -DskipTests' - testy byly pomijane.)
RUN mvn --batch-mode clean verify

# ---- Deploy Stage ----
FROM eclipse-temurin:11-jre

# Copy the built JAR from the build stage
COPY --from=build /app/target/thymeleaf-0.0.1-SNAPSHOT.jar /app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "/app.jar"]
