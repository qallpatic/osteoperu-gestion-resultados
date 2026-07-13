# ============================================================
#  OSTEOPERU Resultados - Backend - Dockerfile multi-stage
#  ------------------------------------------------------------
#  Stage 1 (build): compila el JAR con Maven + JDK 21 DENTRO de Docker.
#                   El host NO necesita tener Java ni Maven instalados.
#  Stage 2 (runtime): solo el JRE 21 con el JAR ya compilado.
# ============================================================

# ---------- Stage 1: build con Maven + JDK 21 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache de dependencias: copio solo pom.xml primero. Mientras no cambie,
# Docker reutiliza la capa con todas las deps descargadas.
COPY pom.xml ./
RUN mvn -B -ntp -q dependency:go-offline || true

# Ahora copio el codigo y compilo.
COPY src ./src
RUN mvn -B -ntp -DskipTests package \
    && cp target/back-envio-resultados-api-*.jar /tmp/app.jar

# ---------- Stage 2: runtime con JRE 21 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copia el JAR compilado en el stage anterior.
COPY --from=build /tmp/app.jar /app/app.jar

# Ambiente por defecto (perfil y volumen).
# SPRING_PROFILES_ACTIVE se sobreescribe por docker-compose / .env.
ENV SPRING_PROFILES_ACTIVE=server
ENV STORAGE_BASE_PATH=/app/uploads
VOLUME ["/app/uploads"]

EXPOSE 7090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]