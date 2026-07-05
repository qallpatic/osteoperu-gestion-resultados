# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Ambiente por defecto en el contenedor
ENV SPRING_PROFILES_ACTIVE=server
# Carpeta 'uploads' de resultados (se monta un volumen aqui desde el host)
ENV STORAGE_BASE_PATH=/app/uploads
VOLUME ["/app/uploads"]

EXPOSE 7090
ENTRYPOINT ["java", "-jar", "app.jar"]
