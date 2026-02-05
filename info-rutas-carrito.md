# Información Técnica: Rutas del Carrito

**Objetivo**: Recolección de información técnica del estado actual del proyecto.  
**Fecha**: 27 de enero de 2026  
**Alcance**: API Gateway, Ventas (CarritoController) y Front-end

---

## 1) API Gateway – Estructura Real del YAML

### 1.1 Archivo Local

**Path completo**:  
`c:/Users/pelud/OneDrive/Documentos/UNRN/Taller de Tecnologías y Producción de Software/apigateway-main/src/main/resources/application.yml`

**Nivel exacto de las rutas**:  
`spring.cloud.gateway.server.webflux.routes`

**Bloque COMPLETO**:

```yaml
spring:
  application.name: api-gateway

  # Configuración de OAuth2 Resource Server (JWT con Keycloak)
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9090/realms/videoclub
          jwk-set-uri: http://localhost:9090/realms/videoclub/protocol/openid-connect/certs

  cloud:
    gateway:
      server:
        webflux:
          globalcors:
            cors-configurations:
              "[/**]": # Configuración CORS para todos los endpoints
                allowedOrigins: "http://localhost:5173" # Origen permitido para solicitudes CORS
                allowedMethods: "*" # Métodos HTTP permitidos
                allowedHeaders: "*" # Cabeceras permitidas
                exposedHeaders: "Authorization" # Exponer header Authorization
                allowCredentials: true # Permitir credenciales (cookies, auth headers)
          routes:
            - id: catalogo # Servicio de Catálogo de Películas
              uri: http://localhost:8081 # Puerto del servicio Catálogo
              predicates:
                - Path=/api/peliculas/** # Rutas de películas
              filters:
                - StripPrefix=1 # Elimina /api del path
            - id: rating # Servicio de Rating de Películas
              uri: http://localhost:8082 # Puerto del servicio Rating
              predicates:
                - Path=/api/ratings/** # Rutas de ratings
              filters:
                - StripPrefix=1 # Elimina /api del path
            - id: ventas-carrito # Servicio de Ventas (Carrito)
              uri: http://localhost:8083 # Puerto del servicio Ventas
              predicates:
                - Path=/api/carrito/** # Rutas de carrito
              filters:
                - StripPrefix=1 # Elimina /api del path
            - id: keycloak # Servicio de autenticación Keycloak
              uri: http://localhost:9090 # Puerto de Keycloak
              predicates:
                - Path=/auth/**,/realms/** # Rutas de Keycloak
              filters:
                - RewritePath=/auth/(?<segment>.*), /$\{segment} # Reescribe /auth/* a /*
```

**Confirmación**:  
✅ Las rutas están declaradas bajo `spring.cloud.gateway.server.webflux.routes`

---

### 1.2 Archivo Docker

**Path completo**:  
`c:/Users/pelud/OneDrive/Documentos/UNRN/Taller de Tecnologías y Producción de Software/apigateway-main/src/main/resources/application-docker.yml`

**Nivel exacto de las rutas**:  
`spring.cloud.gateway.server.webflux.routes`

**Bloque COMPLETO**:

```yaml
spring:
  application.name: api-gateway

  # Configuración de OAuth2 Resource Server (JWT)
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://keycloak:8080/realms/videoclub
          jwk-set-uri: http://keycloak:8080/realms/videoclub/protocol/openid-connect/certs

  cloud:
    gateway:
      server:
        webflux:
          globalcors:
            cors-configurations:
              "[/**]":
                allowedOrigins: "http://localhost:5173"
                allowedMethods: "*"
                allowedHeaders: "*"
                exposedHeaders: "Authorization"
                allowCredentials: true
          routes:
            - id: catalogo
              uri: http://catalogo-backend:8080
              predicates:
                - Path=/api/peliculas/**
              filters:
                - StripPrefix=1
            - id: rating
              uri: http://rating-service:8080
              predicates:
                - Path=/api/ratings/**
              filters:
                - StripPrefix=1
            - id: ventas-carrito
              uri: http://ventas-service:8083
              predicates:
                - Path=/api/carrito/**
              filters:
                - StripPrefix=1
            - id: keycloak
              uri: http://keycloak:8080
              predicates:
                - Path=/auth/**,/realms/**
```

**Confirmación**:  
✅ Las rutas están declaradas bajo `spring.cloud.gateway.server.webflux.routes`

---

## 2) Ventas – Mappings Exactos del CarritoController

### 2.1 Path completo del archivo

`c:/Users/pelud/OneDrive/Documentos/UNRN/Taller de Tecnologías y Producción de Software/el-almacen-de-peliculas-online-ventas/src/main/java/unrn/api/CarritoController.java`

### 2.2 Package

`package unrn.api;`

### 2.3 Anotaciones a nivel clase

```java
@RestController
@RequestMapping("/carrito")
public class CarritoController {
```

### 2.4 Anotaciones y métodos completos

```java
@GetMapping
public ResponseEntity<CarritoDTO> verCarrito() {
    CarritoDTO carrito = carritoService.verCarrito();
    return ResponseEntity.ok(carrito);
}

@PostMapping("/items")
public ResponseEntity<CarritoDTO> agregarPelicula(@RequestBody AgregarPeliculaRequest request) {
    CarritoDTO carrito = carritoService.agregarPelicula(request);
    return ResponseEntity.ok(carrito);
}

@DeleteMapping("/items/{peliculaId}")
public ResponseEntity<CarritoDTO> eliminarPelicula(@PathVariable String peliculaId) {
    CarritoDTO carrito = carritoService.eliminarPelicula(peliculaId);
    return ResponseEntity.ok(carrito);
}
```

### 2.5 Resumen de paths finales

| Método HTTP | Path del Controller                | Path completo final           |
| ----------- | ---------------------------------- | ----------------------------- |
| GET         | `/carrito` + (empty)               | `/carrito`                    |
| POST        | `/carrito` + `/items`              | `/carrito/items`              |
| DELETE      | `/carrito` + `/items/{peliculaId}` | `/carrito/items/{peliculaId}` |

**Confirmación**:  
✅ NO hay duplicación de `/carrito` entre clase y métodos  
✅ La anotación a nivel clase establece el prefijo base `/carrito`  
✅ Los métodos agregan paths relativos: (empty), `/items`, `/items/{peliculaId}`

---

## 3) Front-end – Archivos que Consumen el Carrito

### 3.1 Archivo de API del carrito

**Path completo**:  
`c:/Users/pelud/OneDrive/Documentos/UNRN/Taller de Tecnologías y Producción de Software/el-almacen-de-peliculas-online-front-end/src/api/carrito.js`

**Descripción**:  
Contiene las funciones para consumir los endpoints del carrito:

- `fetchCarrito()`
- `agregarAlCarrito()`
- `eliminarDelCarrito()`

---

### 3.2 Componente que renderiza el carrito

**Path completo**:  
`c:/Users/pelud/OneDrive/Documentos/UNRN/Taller de Tecnologías y Producción de Software/el-almacen-de-peliculas-online-front-end/src/pages/Carrito.jsx`

**Descripción**:  
Página principal que muestra el carrito del usuario.  
Importa: `fetchCarrito`, `eliminarDelCarrito` desde `../api/carrito`

---

### 3.3 Componente con botón "Agregar al carrito" (catálogo)

**Path completo**:  
`c:/Users/pelud/OneDrive/Documentos/UNRN/Taller de Tecnologías y Producción de Software/el-almacen-de-peliculas-online-front-end/src/components/ProductCard.jsx`

**Nombre real del componente**:  
`ProductCard`

**Descripción**:  
Tarjeta de película en el catálogo que incluye:

- Botón para agregar película al carrito
- Manejo de modal de login
- Importa: `agregarAlCarrito` desde `../api/carrito`

---

## 4) Perfiles Activos

### 4.1 API Gateway

#### Perfil Local

**Método de activación**:  
Ejecutar manualmente con Maven:

```bash
mvn spring-boot:run
```

Por defecto, Spring Boot usa `application.yml` sin perfil explícito.  
Para forzar perfil `local`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

O con variable de entorno:

```bash
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

---

#### Perfil Docker

**Método de activación**:  
Configurado en `docker-compose-full.yml`:

```yaml
# API Gateway
api-gateway:
  build:
    context: ../apigateway-main
    dockerfile: Dockerfile
  image: api-gateway:latest
  container_name: api-gateway
  restart: unless-stopped
  depends_on:
    keycloak:
      condition: service_healthy
    catalogo-backend:
      condition: service_healthy
    rating-service:
      condition: service_healthy
  ports:
    - "9500:9500"
  environment:
    SPRING_PROFILES_ACTIVE: docker # <-- Activación del perfil docker
  networks:
    - peliculas-net
```

**Path de docker-compose-full.yml**:  
`c:/Users/pelud/OneDrive/Documentos/UNRN/Taller de Tecnologías y Producción de Software/el-almacen-de-peliculas-online/docker-compose-full.yml`

**Línea de configuración**: Línea 190  
**Variable de entorno**: `SPRING_PROFILES_ACTIVE: docker`

---

### 4.2 Ventas Service

#### Perfil Local

**Método de activación**:  
Ejecutar manualmente con Maven desde el directorio del servicio:

```bash
cd el-almacen-de-peliculas-online-ventas
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

Sin perfil explícito, usa `application.properties` por defecto.

---

#### Perfil Docker

**Método de activación standalone**:  
Configurado en `docker-compose.yml` del servicio:

```yaml
ventas-service:
  build: .
  container_name: ventas-service
  environment:
    # Perfil Spring que usa application-docker.properties
    SPRING_PROFILES_ACTIVE: docker # <-- Activación del perfil docker
    # Puerto del servicio (debe coincidir con application-docker.properties)
    SERVER_PORT: 8083
  ports:
    - "8083:8083"
```

**Path de docker-compose.yml**:  
`c:/Users/pelud/OneDrive/Documentos/UNRN/Taller de Tecnologías y Producción de Software/el-almacen-de-peliculas-online-ventas/docker-compose.yml`

**Línea de configuración**: Línea 20  
**Variable de entorno**: `SPRING_PROFILES_ACTIVE: docker`

---

**Método de activación integrado**:  
Cuando se integra a `docker-compose-full.yml`, se debe agregar la misma configuración:

```yaml
ventas-service:
  build:
    context: ../el-almacen-de-peliculas-online-ventas
    dockerfile: Dockerfile
  # ...
  environment:
    SPRING_PROFILES_ACTIVE: docker # <-- Activación del perfil docker
```

---

## Resumen de Flujo Actual

### Entorno Local

1. **Gateway recibe**: `GET http://localhost:9500/api/carrito`
2. **Gateway aplica**:
   - Predicate: `Path=/api/carrito/**` ✅ Match
   - Filter: `StripPrefix=1` (elimina `/api`)
   - URI: `http://localhost:8083`
3. **Gateway envía a Ventas**: `GET http://localhost:8083/carrito`
4. **Ventas procesa**: `@RequestMapping("/carrito")` + `@GetMapping` = `/carrito` ✅ Match

---

### Entorno Docker

1. **Gateway recibe**: `GET http://api-gateway:9500/api/carrito`
2. **Gateway aplica**:
   - Predicate: `Path=/api/carrito/**` ✅ Match
   - Filter: `StripPrefix=1` (elimina `/api`)
   - URI: `http://ventas-service:8083`
3. **Gateway envía a Ventas**: `GET http://ventas-service:8083/carrito`
4. **Ventas procesa**: `@RequestMapping("/carrito")` + `@GetMapping` = `/carrito` ✅ Match

---

## Observaciones Técnicas

### ✅ Configuración Correcta

1. **Gateway routes** están bajo la estructura estándar:  
   `spring.cloud.gateway.server.webflux.routes`

2. **Filtro StripPrefix=1** está configurado correctamente para eliminar `/api`

3. **NO hay duplicación** de paths en `CarritoController`:
   - Clase: `/carrito`
   - Métodos: (empty), `/items`, `/items/{peliculaId}`

4. **Perfiles** están correctamente activados vía `SPRING_PROFILES_ACTIVE`

---

### ⚠️ Puntos de Atención

1. **Front-end** puede estar construyendo URLs con el patrón antiguo:  
   `/api/clientes/{clienteId}/carrito/...`  
   Debe actualizarse a: `/api/carrito/...`

2. **JWT Authentication**: El servicio Ventas extrae el `clienteId` del token JWT (`preferred_username`), no de la URL.

3. **CORS**: Gateway está configurado para aceptar solicitudes desde `http://localhost:5173` (Vite dev server)

---

## Fin del Documento
