# =====================================================
# MyTrips – Multi-stage Dockerfile
# Compatible AMD64 + ARM64 (Raspberry Pi 4/5)
# =====================================================

# ─── Stage 1 : Build ──────────────────────────────
FROM --platform=linux/arm64 eclipse-temurin:25.0.2_10-jdk-alpine AS build

WORKDIR /app

# Copie du wrapper et du POM en premier pour le cache des dépendances
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw

# Téléchargement des dépendances (layer mis en cache)
RUN ./mvnw dependency:go-offline -B -q

# Compilation
COPY src ./src
RUN ./mvnw package -DskipTests -B -q

# ─── Stage 2 : Extraction des layers Spring Boot ──
FROM --platform=linux/arm64 eclipse-temurin:25.0.2_10-jdk-alpine AS extractor

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Découpe le JAR en layers (dépendances stables / code applicatif volatile)
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ─── Stage 3 : Image finale ─────────────────────
# eclipse-temurin:21-jre-jammy supporte nativement ARM64 (linux/arm64/v8)
FROM --platform=linux/arm64 eclipse-temurin:25.0.2_10-jre-jammy

WORKDIR /app

# Layers copiées du plus stable au plus volatile :
# un rebuild ne retransmet que la couche "application/" si seul ton code a changé
COPY --from=extractor /app/extracted/dependencies/          ./
COPY --from=extractor /app/extracted/spring-boot-loader/    ./
COPY --from=extractor /app/extracted/snapshot-dependencies/ ./
COPY --from=extractor /app/extracted/application/           ./

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=docker", \
    "org.springframework.boot.loader.launch.JarLauncher"]
