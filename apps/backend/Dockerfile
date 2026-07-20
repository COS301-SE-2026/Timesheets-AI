FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY pom.xml .
#RUN mvn dependency:go-offline
#temporarily commented out mvn dependency:go-offline to prevent build hangs with network connection issues
#uncomment it out if you want faster builds, but note it requires a stable connection

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# FIX: added curl so Spring Boot healthcheck works in container
RUN apk add --no-cache curl

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]