FROM eclipse-temurin:17-jdk as build
WORKDIR /app
COPY . .
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests=true

# Production
FROM eclipse-temurin:17-jdk as production
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

# Tests
FROM eclipse-temurin:17-jdk as development
WORKDIR /app
COPY . .
RUN chmod +x ./mvnw