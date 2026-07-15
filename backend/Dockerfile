# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Cache dependencies separately from source for faster rebuilds
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Non-root user for security
RUN useradd -ms /bin/bash pager
COPY --from=build /app/target/*.jar /app/app.jar

# SQLite DB lives on a mounted volume so it survives container restarts/redeploys
ENV PAGER_DB_PATH=/data/pager.db
RUN mkdir -p /data && chown pager:pager /data
VOLUME ["/data"]

USER pager
EXPOSE 9090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
