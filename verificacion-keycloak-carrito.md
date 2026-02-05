# Verificación de Autenticación JWT con Keycloak para Carrito

**Fecha:** 2026-01-28  
**Objetivo:** Garantizar que POST /api/carrito/items funciona con JWT válido y devuelve 401/403 sin JWT (no 404)

---

## CONFIGURACIÓN ACTUAL

### Gateway (localhost:9500)

- **Ruta configurada:** `/api/carrito/**`
- **Destino:** `http://localhost:8083` (local) / `http://ventas-service:8083` (docker)
- **StripPrefix:** 1 (elimina `/api`)
- **Seguridad:** `.pathMatchers("/api/carrito/**").authenticated()`
- **JWT Issuer (local):** `http://localhost:9090/realms/videoclub`
- **JWT Issuer (docker):** `http://keycloak:8080/realms/videoclub`

### Ventas (localhost:8083)

- **Endpoint:** `POST /carrito/items`
- **Controller:** `@RequestMapping("/carrito")`
- **Seguridad:** `.requestMatchers("/carrito/**").authenticated()`
- **JWT Issuer (local):** `http://localhost:9090/realms/videoclub`
- **JWT Issuer (docker):** `http://keycloak:8080/realms/videoclub`

### Keycloak

- **URL (local):** `http://localhost:9090`
- **URL (docker):** `http://keycloak:8080`
- **Realm:** `videoclub`
- **Token Endpoint:** `/realms/videoclub/protocol/openid-connect/token`

---

## A) VERIFICACIÓN DE KEYCLOAK

### 1. Confirmar Realm y Client

Acceder a Keycloak Admin Console:

```bash
# Abrir en navegador
http://localhost:9090/admin
# Credenciales: admin / admin
```

**Verificar:**

- [ ] Realm `videoclub` existe
- [ ] Client configurado (ejemplo: `videoclub-client`)
- [ ] Direct Access Grants habilitado (para testing con password grant)
- [ ] Usuario de prueba existe (ejemplo: `testuser` / `test123`)

### 2. Habilitar Direct Access Grants (si es necesario)

En Keycloak Admin Console:

1. Ir a `Clients` → seleccionar tu client (ej: `videoclub-client`)
2. Tab `Settings`
3. Sección `Capability config`
4. Habilitar: **Direct access grants**
5. Guardar

⚠️ **NOTA:** Esto es SOLO para testing local. En producción usar Authorization Code Flow.

---

## B) OBTENER TOKEN REAL DE KEYCLOAK

### Variables de Configuración

Antes de ejecutar los comandos, configura estas variables según tu configuración de Keycloak:

```powershell
# PowerShell
$KEYCLOAK_URL = "http://localhost:9090"
$REALM = "videoclub"
$CLIENT_ID = "videoclub-client"  # ⚠️ AJUSTAR según tu configuración
$USERNAME = "testuser"            # ⚠️ AJUSTAR según tu usuario
$PASSWORD = "test123"             # ⚠️ AJUSTAR según tu password
```

### Comando para Obtener Token

```powershell
# PowerShell
$response = Invoke-RestMethod -Uri "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" `
  -Method POST `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type = "password"
    client_id = $CLIENT_ID
    username = $USERNAME
    password = $PASSWORD
  }

# Extraer access_token
$ACCESS_TOKEN = $response.access_token
Write-Host "Token obtenido: $($ACCESS_TOKEN.Substring(0,50))..."
```

**Alternativa con curl (Git Bash / WSL):**

```bash
curl -X POST "http://localhost:9090/realms/videoclub/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=videoclub-client" \
  -d "username=testuser" \
  -d "password=test123" | jq -r '.access_token'
```

**Resultado esperado:**

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "token_type": "Bearer",
  ...
}
```

---

## C) PROBAR VENTAS DIRECTO CON TOKEN

### Objetivo

Descartar que el problema sea del Gateway. Si Ventas directo falla, el problema está en Ventas, no en el Gateway.

### Test 1: POST con Token Válido

```powershell
# PowerShell
$body = @{
  peliculaId = "1"
  titulo = "The Matrix"
  precioUnitario = 100.00
  cantidad = 1
} | ConvertTo-Json

$headers = @{
  "Authorization" = "Bearer $ACCESS_TOKEN"
  "Content-Type" = "application/json"
}

Invoke-RestMethod -Uri "http://localhost:8083/carrito/items" `
  -Method POST `
  -Headers $headers `
  -Body $body
```

**Alternativa con curl:**

```bash
curl -X POST http://localhost:8083/carrito/items \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "peliculaId": "1",
    "titulo": "The Matrix",
    "precioUnitario": 100.00,
    "cantidad": 1
  }'
```

**Resultado esperado:** ✅ **200 OK** (o 4xx por validación de negocio, pero NO 404)

**Si devuelve 404:**

- ❌ El endpoint `/carrito/items` no está mapeado
- Verificar logs de Ventas: `Mapped "{[POST /carrito/items]}"`
- Verificar que no existe `server.servlet.context-path` ni `spring.mvc.servlet.path`

**Si devuelve 401/403:**

- ❌ Problema con validación JWT en Ventas
- Verificar `spring.security.oauth2.resourceserver.jwt.issuer-uri` en `application-local.properties`
- Debe ser: `http://localhost:9090/realms/videoclub`

### Test 2: POST sin Token

```powershell
# PowerShell
Invoke-RestMethod -Uri "http://localhost:8083/carrito/items" `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body $body
```

**Resultado esperado:** ✅ **401 Unauthorized** o **403 Forbidden** (NO 404)

**Si devuelve 404:**

- ❌ Spring Security no está protegiendo la ruta correctamente
- Verificar `SecurityConfig.java` en Ventas
- Debe tener: `.requestMatchers("/carrito/**").authenticated()`

---

## D) PROBAR GATEWAY CON TOKEN

### Objetivo

Verificar que el Gateway enruta correctamente a Ventas con el JWT.

### Test 1: POST vía Gateway con Token

```powershell
# PowerShell
$headers = @{
  "Authorization" = "Bearer $ACCESS_TOKEN"
  "Content-Type" = "application/json"
}

$body = @{
  peliculaId = "1"
  titulo = "The Matrix"
  precioUnitario = 100.00
  cantidad = 1
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:9500/api/carrito/items" `
  -Method POST `
  -Headers $headers `
  -Body $body
```

**Resultado esperado:** ✅ **200 OK** (debe llegar a Ventas)

**Si devuelve 404:**

1. Verificar que la ruta del Gateway matchea:

   ```powershell
   # Verificar rutas cargadas en Gateway
   Invoke-RestMethod -Uri "http://localhost:9500/actuator/gateway/routes"
   ```

   Buscar:

   ```json
   {
     "route_id": "ventas-carrito",
     "uri": "http://localhost:8083",
     "predicate": "Paths: [/api/carrito/**], match trailing slash: true",
     "filters": ["[[StripPrefix parts = 1], order = 1]"]
   }
   ```

2. Si la ruta no aparece:
   - Gateway no levantó las rutas correctamente
   - Verificar logs del Gateway al arrancar

### Test 2: POST vía Gateway sin Token

```powershell
# PowerShell
Invoke-RestMethod -Uri "http://localhost:9500/api/carrito/items" `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body $body
```

**Resultado esperado:** ✅ **401 Unauthorized** (Gateway debe rechazar antes de forwardear)

---

## E) VERIFICACIÓN DE LOGS

### Logs del Gateway (arranque)

**Buscar en logs:**

```
RouteDefinitionRouteLocator : Loaded RoutePredicateFactory [Path]
RouteDefinitionRouteLocator : Loaded [ventas-carrito] with predicate [Paths: [/api/carrito/**]]
```

**Si no aparece:**

- `application.yml` mal formateado o no se cargó el perfil correcto
- Verificar: `SPRING_PROFILES_ACTIVE=default` (para local)

### Logs de Ventas (arranque)

**Buscar en logs:**

```
Tomcat started on port(s): 8083 (http)
Started ElAlmacenDePeliculasOnlineVentasApplication in X seconds
RequestMappingHandlerMapping : Mapped "{[/carrito/items],methods=[POST]}" onto public ResponseEntity unrn.api.CarritoController.agregarPelicula(...)
```

**Si no aparece el mapping:**

- El controller no fue escaneado
- Verificar que `@ComponentScan` incluye el paquete `unrn.api`

### Logs durante Request (con logging.level.org.springframework.security=DEBUG)

**Ventas (con token válido):**

```
JwtAuthenticationProvider : Authenticated: Principal: ...
FilterChainProxy : Secured POST /carrito/items
```

**Ventas (sin token):**

```
BearerTokenAuthenticationFilter : Did not process request since did not find bearer token
FilterChainProxy : Request for POST /carrito/items returned 401
```

**Gateway (con token válido):**

```
RoutePredicateHandlerMapping : Route matched: ventas-carrito
FilteringWebHandler : Applying filter: StripPrefixGatewayFilterFactory
LoadBalancerClientFilter : URL selected for load balancing: http://localhost:8083/carrito/items
```

---

## F) CORRECCIÓN DE PROBLEMAS COMUNES

### Problema 1: 404 en Ventas directo con token

**Causa:** Endpoint no mapeado o context-path configurado.

**Solución:**

1. Verificar que `application-local.properties` NO tiene:

   ```properties
   # QUITAR SI EXISTE:
   # server.servlet.context-path=/api
   # spring.mvc.servlet.path=/v1
   ```

2. Confirmar mapping en logs de arranque de Ventas.

### Problema 2: 401/403 en Ventas con token válido

**Causa:** Issuer URI incorrecto o JWK no accesible.

**Solución:**

1. Verificar `application-local.properties`:

   ```properties
   spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9090/realms/videoclub
   ```

2. Probar manualmente que el issuer es accesible:

   ```powershell
   Invoke-RestMethod -Uri "http://localhost:9090/realms/videoclub/.well-known/openid-configuration"
   ```

   Debe devolver JSON con `issuer`, `jwks_uri`, etc.

### Problema 3: Gateway devuelve 404 pero Ventas directo funciona

**Causa:** Ruta no cargada en Gateway o StripPrefix incorrecto.

**Solución:**

1. Verificar actuator del Gateway:

   ```powershell
   Invoke-RestMethod -Uri "http://localhost:9500/actuator/gateway/routes" | ConvertTo-Json -Depth 10
   ```

2. Si `ventas-carrito` no aparece, verificar `application.yml`:

   ```yaml
   spring:
     cloud:
       gateway:
         routes:
           - id: ventas-carrito
             uri: http://localhost:8083
             predicates:
               - Path=/api/carrito/**
             filters:
               - StripPrefix=1
   ```

3. Reiniciar Gateway y verificar logs de arranque.

### Problema 4: Sin token devuelve 404 en lugar de 401/403

**Causa:** Spring Security no intercepta la ruta o la regla está mal configurada.

**En Ventas (`SecurityConfig.java`):**

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/h2-console/**").permitAll()
            .requestMatchers("/carrito/**").authenticated()  // ⚠️ Asegurar esto
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
        .csrf(csrf -> csrf.disable());
    return http.build();
}
```

**En Gateway (`SecurityConfig.java`):**

```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers("/auth/**", "/realms/**").permitAll()
            .pathMatchers("/actuator/health", "/actuator/gateway/**").permitAll()
            .pathMatchers(HttpMethod.GET, "/api/peliculas/**").permitAll()
            .pathMatchers("/api/peliculas/**").authenticated()
            .pathMatchers("/api/ratings/**").authenticated()
            .pathMatchers("/api/carrito/**").authenticated()  // ⚠️ Asegurar esto
            .anyExchange().permitAll()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
    return http.build();
}
```

---

## RESUMEN DE COMANDOS COMPLETOS

### 1. Obtener Token

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:9090/realms/videoclub/protocol/openid-connect/token" `
  -Method POST `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type = "password"
    client_id = "videoclub-client"
    username = "testuser"
    password = "test123"
  }
$ACCESS_TOKEN = $response.access_token
```

### 2. Probar Ventas Directo (con token)

```powershell
$body = '{"peliculaId":"1","titulo":"The Matrix","precioUnitario":100.00,"cantidad":1}'
Invoke-RestMethod -Uri "http://localhost:8083/carrito/items" `
  -Method POST `
  -Headers @{"Authorization"="Bearer $ACCESS_TOKEN";"Content-Type"="application/json"} `
  -Body $body
```

### 3. Probar Ventas Directo (sin token)

```powershell
Invoke-RestMethod -Uri "http://localhost:8083/carrito/items" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body $body
```

### 4. Probar Gateway (con token)

```powershell
Invoke-RestMethod -Uri "http://localhost:9500/api/carrito/items" `
  -Method POST `
  -Headers @{"Authorization"="Bearer $ACCESS_TOKEN";"Content-Type"="application/json"} `
  -Body $body
```

### 5. Probar Gateway (sin token)

```powershell
Invoke-RestMethod -Uri "http://localhost:9500/api/carrito/items" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body $body
```

### 6. Verificar Rutas del Gateway

```powershell
Invoke-RestMethod -Uri "http://localhost:9500/actuator/gateway/routes" | ConvertTo-Json -Depth 10
```

---

## EVIDENCIAS A CAPTURAR

### ✅ Checklist de Evidencias

- [ ] Comando obtener token + respuesta (access_token truncado)
- [ ] Ventas directo con token → 200 OK
- [ ] Ventas directo sin token → 401 Unauthorized
- [ ] Gateway con token → 200 OK
- [ ] Gateway sin token → 401 Unauthorized
- [ ] Logs de Gateway mostrando route match `ventas-carrito`
- [ ] Logs de Ventas mostrando request recibido en `/carrito/items`
- [ ] Output de `/actuator/gateway/routes` mostrando ruta `ventas-carrito`

---

## ARCHIVOS MODIFICADOS

_(Documentar aquí cualquier cambio realizado en archivos de configuración o seguridad)_

### Cambios en Gateway

**Archivo:** `apigateway-main/src/main/resources/application.yml`

```diff
# Sin cambios necesarios (configuración actual es correcta)
```

**Archivo:** `apigateway-main/src/main/java/com/videoclub/apigateway/config/SecurityConfig.java`

```diff
# Sin cambios necesarios (configuración actual es correcta)
```

### Cambios en Ventas

**Archivo:** `el-almacen-de-peliculas-online-ventas/src/main/resources/application-local.properties`

```diff
# Sin cambios necesarios (configuración actual es correcta)
```

**Archivo:** `el-almacen-de-peliculas-online-ventas/src/main/java/unrn/security/SecurityConfig.java`

```diff
# Sin cambios necesarios (configuración actual es correcta)
```

---

## CONCLUSIÓN

La configuración actual de Gateway y Ventas es **correcta**:

- Ambos servicios tienen configurado JWT con Keycloak correctamente
- Las rutas de seguridad están protegidas con `.authenticated()`
- El Gateway tiene la ruta `ventas-carrito` configurada con StripPrefix=1

**Pasos siguientes:**

1. Verificar que Keycloak está corriendo y accesible en `http://localhost:9090`
2. Verificar que existe un realm `videoclub` con un client configurado
3. Obtener un token real siguiendo los comandos en sección B
4. Ejecutar las pruebas en orden: C (Ventas directo) → D (Gateway)
5. Capturar logs para evidencia

**Si sigue habiendo 404:**

- Verificar que ambos servicios están corriendo (no solo Keycloak)
- Confirmar que Ventas está escuchando en puerto 8083
- Confirmar que Gateway está escuchando en puerto 9500
- Revisar logs de arranque de ambos servicios
