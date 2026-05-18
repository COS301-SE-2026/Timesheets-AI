FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# pom.xml specifies the dependencies, plugins, build configuration and we downloads Maven dependencies 
COPY pom.xml . 
RUN mvn dependency:go-offline

# copies all the java code into a container
COPY src ./src

# build the application 
# clean - delete the old builds
# package - compilies the code and create a .jar
# -DsipTests - dont runs the tests like integration or unit tests because the CI pipeline will so help to make the build faster 
RUN mvn clean package -DskipTests
 
# jre alphine is used to minimize size
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# this will get us the built JAR so the final images will have the compilied app, no source code or maven 
COPY --from=build /app/target/*.jar app.jar 

# the port used since frontend uses 8080 to avoid clashing 
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]

# uses a docker imahe that has maven 3.9, JDK 17 