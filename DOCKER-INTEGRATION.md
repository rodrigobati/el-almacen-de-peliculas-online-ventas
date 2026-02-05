# Guía de Integración con docker-compose-full.yml

Esta guía explica cómo integrar el servicio de Ventas en el archivo `docker-compose-full.yml` del repo principal.

## Paso 1: Agregar el servicio ventas-service

Agregar la siguiente configuración en el archivo `docker-compose-full.yml` (en la carpeta raíz del proyecto principal), después del servicio `rating-service`:

```yaml
# Servicio de Ventas (Carrito de Compras)
ventas-service:
  build:
    context: ../el-almacen-de-peliculas-online-ventas
    dockerfile: Dockerfile
  image: el-almacen-de-peliculas-online-ventas:latest
  container_name: ventas-service
  restart: unless-stopped
  ports:
    - "8083:8083"
  environment:
    SPRING_PROFILES_ACTIVE: docker
    SERVER_PORT: 8083
  networks:
    - peliculas-net
```

## Paso 2: Actualizar el API Gateway

Agregar la ruta de ventas en las variables de entorno del servicio `api-gateway`:

```yaml
api-gateway:
  # ... configuración existente ...
  environment:
    # ... variables existentes ...

    # Nueva ruta para Ventas
    SPRING_CLOUD_GATEWAY_ROUTES_3_ID: ventas
    SPRING_CLOUD_GATEWAY_ROUTES_3_URI: http://ventas-service:8083
    SPRING_CLOUD_GATEWAY_ROUTES_3_PREDICATES_0: Path=/api/ventas/**,/clientes/**
    SPRING_CLOUD_GATEWAY_ROUTES_3_FILTERS_0: StripPrefix=1
```

**Alternativa (recomendada):** Editar directamente el archivo `application-docker.yml` del gateway:

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ... rutas existentes ...

        - id: ventas
          uri: http://ventas-service:8083
          predicates:
            - Path=/api/ventas/**,/clientes/**
          filters:
            - StripPrefix=1
```

## Paso 3: Verificar la red

Asegurar que el nombre de la red sea consistente. El `docker-compose-full.yml` usa `peliculas-net`, por lo que el servicio de ventas debe usar esa misma red:

```yaml
networks:
  peliculas-net:
    driver: bridge
```

## Paso 4: Levantar el ecosistema completo

Desde la carpeta que contiene `docker-compose-full.yml`:

```bash
# Construir todas las imágenes
docker-compose -f docker-compose-full.yml build

# Levantar todos los servicios
docker-compose -f docker-compose-full.yml up -d

# Ver logs de todos los servicios
docker-compose -f docker-compose-full.yml logs -f

# Ver logs solo del servicio de ventas
docker-compose -f docker-compose-full.yml logs -f ventas-service
```

## Paso 5: Verificar la integración

Una vez levantado el ecosistema completo, verificar que el servicio de ventas esté accesible:

```bash
# Directo al servicio (sin gateway)
curl http://localhost:8083/clientes/test-001/carrito

# A través del API Gateway
curl http://localhost:9500/api/ventas/clientes/test-001/carrito
```

## Configuración final sugerida en docker-compose-full.yml

```yaml
services:
  # ... servicios existentes ...

  # Servicio de Ventas (Carrito de Compras)
  ventas-service:
    build:
      context: ../el-almacen-de-peliculas-online-ventas
      dockerfile: Dockerfile
    image: el-almacen-de-peliculas-online-ventas:latest
    container_name: ventas-service
    restart: unless-stopped
    ports:
      - "8083:8083"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SERVER_PORT: 8083
    networks:
      - peliculas-net

  api-gateway:
    # ... configuración existente ...
    depends_on:
      catalogo-backend:
        condition: service_healthy
      rating-service:
        condition: service_healthy
      ventas-service:
        condition: service_started # Sin healthcheck por ahora
      keycloak:
        condition: service_healthy
    # ... resto de la configuración ...
```

## Resumen de puertos

| Servicio           | Puerto Host | Puerto Contenedor |
| ------------------ | ----------- | ----------------- |
| Catálogo MySQL     | 3307        | 3306              |
| Rating MySQL       | 3308        | 3306              |
| RabbitMQ           | 5672, 15672 | 5672, 15672       |
| Keycloak           | 9090        | 8080              |
| Catálogo Backend   | 8081        | 8080              |
| Rating Service     | 8082        | 8080              |
| **Ventas Service** | **8083**    | **8083**          |
| API Gateway        | 9500        | 9500              |

## Nombres de servicio para comunicación interna

Los servicios se comunican entre sí usando estos nombres de host:

- `catalogo-mysql:3306`
- `rating-mysql:3306`
- `shared-rabbitmq:5672`
- `keycloak:8080`
- `catalogo-backend:8080`
- `rating-service:8080`
- `ventas-service:8083`
- `api-gateway:9500`

## Notas importantes

1. **Base de datos:** El servicio de Ventas usa H2 en memoria (stateless). Los datos del carrito se pierden al reiniciar.

2. **Healthcheck:** Si se requiere un healthcheck robusto, agregar Spring Boot Actuator al proyecto de ventas.

3. **Dependencias:** El servicio de ventas no depende de MySQL ni RabbitMQ actualmente, pero podría agregarse en el futuro para persistencia o eventos.

4. **CORS:** Si el front-end necesita acceso directo al servicio de ventas (sin pasar por el gateway), configurar CORS en el servicio.
