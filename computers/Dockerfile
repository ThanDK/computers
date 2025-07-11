# Stage 1: Build the application using Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only the pom.xml to download dependencies.
# This step is only re-run if pom.xml changes.
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Build the application. This creates the .jar file.
# We clean the package to ensure a fresh build.
RUN mvn clean package -DskipTests

# Stage 2: Create the final, minimal runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Expose the port the application will run on
EXPOSE 8080

# Copy the executable .jar from the build stage
COPY --from=build /app/target/computers-0.0.1-SNAPSHOT.jar app.jar

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]