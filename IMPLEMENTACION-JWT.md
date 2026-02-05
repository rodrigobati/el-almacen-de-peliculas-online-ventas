# Implementación de Autenticación JWT en Ventas

## ✅ Cambios Realizados

### 1. API Gateway - Routing para Carrito

#### `apigateway-main/src/main/resources/application.yml` (Local)

```yaml
- id: ventas-carrito # Servicio de Ventas (Carrito)
  uri: http://localhost:8083 # Puerto del servicio Ventas
  predicates:
    - Path=/api/carrito/** # Rutas de carrito
  filters:
    - StripPrefix=1 # Elimina /api del path
```

#### `apigateway-main/src/main/resources/application-docker.yml` (Docker)

```yaml
- id: ventas-carrito
  uri: http://ventas-service:8083
  predicates:
    - Path=/api/carrito/**
  filters:
    - StripPrefix=1
```

**Efecto:** El gateway ahora redirige `POST /api/carrito/items` → `http://localhost:8083/carrito/items`

---

### 2. Ventas - Dependencias de Seguridad

#### `pom.xml`

Agregadas las siguientes dependencias:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

---

### 3. Ventas - Configuración de Seguridad

#### Nuevo: `src/main/java/unrn/security/SecurityConfig.java`

```java
package unrn.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/carrito/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            )
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
```

**Efecto:**

- `/carrito/**` requiere autenticación JWT
- `/actuator/health` y `/h2-console/**` son públicos
- CSRF deshabilitado (típico en APIs REST)
- Headers frame options deshabilitado para H2 Console

---

### 4. Ventas - Configuración JWT

#### `src/main/resources/application-docker.properties`

Agregado:

```properties
# ========================================
# SEGURIDAD - OAuth2 JWT (Keycloak)
# ========================================
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://keycloak:8080/realms/videoclub
```

#### Nuevo: `src/main/resources/application-local.properties`

```properties
# Para usar este perfil: SPRING_PROFILES_ACTIVE=local
spring.application.name=ventas-service
server.port=8083

# Base de datos H2
spring.datasource.url=jdbc:h2:mem:ventasdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# Logging
logging.level.root=INFO
logging.level.unrn=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n

# SEGURIDAD - OAuth2 JWT (Keycloak)
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9090/realms/videoclub
```

**Uso:**

- Docker: usa perfil `docker` → `SPRING_PROFILES_ACTIVE=docker`
- Local: usa perfil `local` → `SPRING_PROFILES_ACTIVE=local`

---

### 5. Ventas - Extracción de Usuario desde JWT

#### `src/main/java/unrn/service/CarritoService.java`

**Cambios principales:**

1. **Imports agregados:**

```java
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
```

2. **Constantes actualizadas:**

```java
static final String ERROR_USUARIO_NO_AUTENTICADO = "Usuario no autenticado";
// Eliminadas: ERROR_CLIENTE_ID_NULO, ERROR_CLIENTE_ID_VACIO
```

3. **Métodos modificados (sin parámetro clienteId):**

```java
public CarritoDTO verCarrito() {
    String clienteId = obtenerClienteIdAutenticado();
    Carrito carrito = carritoRepository.obtenerDe(clienteId);
    return mapearADTO(carrito);
}

public CarritoDTO agregarPelicula(AgregarPeliculaRequest request) {
    assertRequestNoNulo(request);
    String clienteId = obtenerClienteIdAutenticado();
    // ...
}

public CarritoDTO eliminarPelicula(String peliculaId) {
    String clienteId = obtenerClienteIdAutenticado();
    // ...
}
```

4. **Nuevo método de extracción:**

```java
private String obtenerClienteIdAutenticado() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null || !auth.isAuthenticated()) {
        throw new RuntimeException(ERROR_USUARIO_NO_AUTENTICADO);
    }

    // Si es JwtAuthenticationToken, obtener el claim preferred_username
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
        String preferredUsername = jwtAuth.getToken().getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }
    }

    // Fallback: usar getName()
    String username = auth.getName();
    if (username == null || username.isBlank()) {
        throw new RuntimeException(ERROR_USUARIO_NO_AUTENTICADO);
    }

    return username;
}
```

---

### 6. Ventas - Controller Actualizado

#### `src/main/java/unrn/api/CarritoController.java`

**Cambios:**

1. **RequestMapping sin clienteId:**

```java
@RequestMapping("/carrito")  // Antes: "/clientes/{clienteId}/carrito"
```

2. **Endpoints sin @PathVariable clienteId:**

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

---

## 🔄 Flujo Completo

### Antes (con clienteId en URL - ❌ 404)

```
Front → POST /api/clientes/patito/carrito/items
    ↓
Gateway (9500) → ❌ No encuentra ruta
    ↓
404 Not Found
```

### Ahora (con JWT - ✅ Funcional)

```
Front → POST /api/carrito/items + Authorization: Bearer <token>
    ↓
Gateway (9500) → Matchea /api/carrito/** → StripPrefix=1
    ↓
Forward → http://localhost:8083/carrito/items + Authorization: Bearer <token>
    ↓
Ventas SecurityConfig → Valida JWT con Keycloak
    ↓
CarritoController → CarritoService.agregarPelicula()
    ↓
obtenerClienteIdAutenticado() → Extrae "preferred_username" del JWT
    ↓
Usa ese username como clienteId en el modelo
    ↓
✅ 200 OK + CarritoDTO
```

---

## ✅ Checklist de Pruebas

### 1. Verificar que el Gateway está corriendo

```bash
curl http://localhost:9500/actuator/health
# Debe retornar: {"status":"UP"}
```

### 2. Verificar que Ventas está corriendo con el perfil local

```bash
# En el directorio de ventas
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run

# O en Windows PowerShell:
$env:SPRING_PROFILES_ACTIVE="local"; mvn spring-boot:run
```

### 3. Obtener un token de Keycloak

```bash
# Obtener token (reemplazar USER y PASSWORD)
curl -X POST http://localhost:9090/realms/videoclub/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=web" \
  -d "username=USER" \
  -d "password=PASSWORD" \
  | jq -r '.access_token'

# Guardar el token en una variable
TOKEN="<pegar_token_aquí>"
```

### 4. Probar sin token (debe retornar 401/403)

```bash
# Sin token - debe fallar con 401 Unauthorized
curl -v http://localhost:9500/api/carrito

# Respuesta esperada:
# < HTTP/1.1 401 Unauthorized
```

### 5. Probar con token válido

#### Ver carrito (GET)

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:9500/api/carrito

# Respuesta esperada:
# {"items":[],"total":0}
```

#### Agregar película al carrito (POST)

```bash
curl -X POST http://localhost:9500/api/carrito/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "peliculaId": "pel-001",
    "titulo": "Inception",
    "precioUnitario": 15.99,
    "cantidad": 1
  }'

# Respuesta esperada:
# {
#   "items": [
#     {
#       "peliculaId": "pel-001",
#       "titulo": "Inception",
#       "precioUnitario": 15.99,
#       "cantidad": 1,
#       "subtotal": 15.99
#     }
#   ],
#   "total": 15.99
# }
```

#### Eliminar película del carrito (DELETE)

```bash
curl -X DELETE http://localhost:9500/api/carrito/items/pel-001 \
  -H "Authorization: Bearer $TOKEN"

# Respuesta esperada:
# {"items":[],"total":0}
```

### 6. Verificar que el usuario se extrae correctamente

Revisar los logs de Ventas, debería aparecer:

```
DEBUG unrn.service.CarritoService - Cliente autenticado: <preferred_username>
```

O agregar temporalmente un log en `obtenerClienteIdAutenticado()`:

```java
String clienteId = obtenerClienteIdAutenticado();
System.out.println("Cliente autenticado: " + clienteId);
```

### 7. Probar desde el Front-End

**Requisitos previos:**

- Usuario debe estar logueado en el front
- El front debe enviar el token en el header `Authorization`

**Actualizar el front-end:**

Modificar `src/api/config.js`:

```javascript
export const API_BASE = "http://localhost:9500/api";
```

Modificar `src/api/carrito.js` para que las URLs no incluyan `clienteId`:

```javascript
// Antes: /clientes/${clienteId}/carrito
// Ahora: /carrito

export async function fetchCarrito() {
  const url = `${API_BASE}/carrito`;
  // ... resto del código
}

export async function agregarAlCarrito(pelicula) {
  const url = `${API_BASE}/carrito/items`;
  // ... resto del código
}

export async function eliminarDelCarrito(peliculaId) {
  const url = `${API_BASE}/carrito/items/${peliculaId}`;
  // ... resto del código
}
```

Y asegurar que se envía el token:

```javascript
const token = keycloak.token;
const headers = {
  "Content-Type": "application/json",
  Authorization: `Bearer ${token}`,
};
```

### 8. Verificar en Docker

```bash
# Levantar todo el ecosistema
docker-compose -f docker-compose-full.yml up -d

# Verificar logs de ventas
docker logs ventas-service

# Probar con token
TOKEN="<token_de_keycloak>"
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:9500/api/carrito
```

---

## 🐛 Troubleshooting

### Error: "401 Unauthorized" incluso con token válido

**Causa:** El issuer-uri no coincide o Keycloak no está accesible desde Ventas.

**Solución:**

1. Verificar que Keycloak está corriendo en el puerto 9090 (local) o como contenedor (docker)
2. Verificar que el issuer-uri es correcto:
   - Local: `http://localhost:9090/realms/videoclub`
   - Docker: `http://keycloak:8080/realms/videoclub`
3. Verificar logs de Ventas para ver errores de conexión con Keycloak

### Error: "Usuario no autenticado" en los logs

**Causa:** El SecurityContext no tiene el Authentication o el token no tiene el claim `preferred_username`.

**Solución:**

1. Verificar que el token es válido: decodificar en jwt.io
2. Verificar que el token tiene el claim `preferred_username`
3. Si no tiene ese claim, el código usará `auth.getName()` como fallback

### Error: "404 Not Found" al llamar a /api/carrito

**Causa:** El Gateway no se reinició después de modificar los archivos de configuración.

**Solución:** Reiniciar el Gateway

### Error: CORS al llamar desde el front

**Causa:** El Gateway tiene CORS configurado para `http://localhost:5173` pero el front corre en otro puerto.

**Solución:** Actualizar `allowedOrigins` en el Gateway para incluir el puerto correcto del front

---

## 📝 Notas Importantes

1. **No pasar clienteId en la URL:** El usuario se extrae automáticamente del JWT. No usar URLs como `/api/clientes/{id}/carrito`.

2. **Token en el front:** El front debe obtener el token de Keycloak y enviarlo en cada request al carrito:

   ```javascript
   headers: {
     "Authorization": `Bearer ${keycloak.token}`
   }
   ```

3. **Profiles de Spring:**
   - Local development: `SPRING_PROFILES_ACTIVE=local`
   - Docker: `SPRING_PROFILES_ACTIVE=docker`

4. **Reiniciar servicios:** Después de cambiar configuración, siempre reiniciar Gateway y Ventas.

5. **H2 Console:** Accesible en `http://localhost:8083/h2-console` (sin autenticación por configuración en SecurityConfig).

---

## ✅ Resumen de Cambios en URLs

| Antes                                                  | Ahora                                    |
| ------------------------------------------------------ | ---------------------------------------- |
| `GET /api/clientes/{id}/carrito`                       | `GET /api/carrito`                       |
| `POST /api/clientes/{id}/carrito/items`                | `POST /api/carrito/items`                |
| `DELETE /api/clientes/{id}/carrito/items/{peliculaId}` | `DELETE /api/carrito/items/{peliculaId}` |

**Autenticación:** Ahora se usa el claim `preferred_username` del JWT en lugar del path parameter.
