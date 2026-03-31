# =====================================================
# MyTrips – Multi-stage Dockerfile
# Compatible ARM64 (Raspberry Pi 4/5)
# =====================================================

# --- Stage 1 : Build ---
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper & POM first for dependency caching
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy sources and build
COPY src ./src
RUN ./mvnw package -DskipTests -B

# --- Stage 2 : Runtime ---
FROM gcr.io/distroless/java21-debian12:nonroot

WORKDIR /app

# Non-root user for security
RUN addgroup -S mytrips && adduser -S mytrips -G mytrips
USER mytrips

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=docker", \
    "-jar", "app.jar"]
