FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /usr/src/app

# Copy the source code
COPY src src
COPY pom.xml .

# Build the application
RUN --mount=type=cache,target=/root/.m2 mvn clean package

FROM openjdk:21 AS runtime

EXPOSE 4573

COPY --from=build /usr/src/app/target/ChatGPTAsterisk-1.0.jar /usr/app/app.jar

WORKDIR /usr/app
ENTRYPOINT ["java", "-jar", "app.jar"]
