# ========================================
# Etapa 1: BUILD
# ========================================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Descargar dependencias (capa cacheada si no cambia pom.xml)
COPY pom.xml .

# Copiar código fuente y compilar
COPY src ./src
RUN mvn -q -Dmaven.test.skip=true package

# ========================================
# Etapa 2: RUNTIME
# ========================================
FROM eclipse-temurin:21-jre
WORKDIR /app

ARG JAR_FILE=target/el-almacen-de-peliculas-online-ventas-0.0.1-SNAPSHOT.jar

# Instalar curl para healthchecks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copiar el jar desde la etapa de build
COPY --from=build /app/${JAR_FILE} app.jar

# Exponer puerto del servicio
EXPOSE 8083

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
