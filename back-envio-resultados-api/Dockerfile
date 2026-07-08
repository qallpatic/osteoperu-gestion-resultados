# ---------- runtime (solo ejecuta el JAR ya compilado) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Coloca un unico .jar junto a este Dockerfile (el ejecutable de Spring Boot).
COPY *.jar app.jar

# Ambiente por defecto en el contenedor
ENV SPRING_PROFILES_ACTIVE=server
# Carpeta 'uploads' de resultados (se monta un volumen aqui desde el host)
ENV STORAGE_BASE_PATH=/app/uploads
VOLUME ["/app/uploads"]

EXPOSE 7090
ENTRYPOINT ["java", "-jar", "app.jar"]
