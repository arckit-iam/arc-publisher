# ── Build stage ────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src
COPY k8s /build/chart

RUN mvn package -q -DskipTests \
 && mv target/arc-publisher-*.jar target/arc-publisher.jar

# ── Runtime stage ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-noble

# Utilisateur non-root
RUN useradd -r appuser
USER appuser

WORKDIR /app

COPY --from=build /build/target/arc-publisher.jar arc-publisher.jar
COPY --from=build /build/chart ./chart

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "arc-publisher.jar"]
