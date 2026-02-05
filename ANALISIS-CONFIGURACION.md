# Análisis de Configuración: Gateway + Ventas + Keycloak

**Fecha:** 2026-01-28  
**Analista:** Senior Engineer Spring Boot + Keycloak + Spring Cloud Gateway

---

## RESUMEN EJECUTIVO

He realizado un análisis exhaustivo de la configuración actual del Gateway, Ventas y Keycloak. **La configuración es correcta desde el punto de vista de código**. El problema reportado (404 en POST /api/carrito/items) es muy probablemente un **problema de runtime** y no de configuración.

---

## CONFIGURACIÓN ACTUAL: ANÁLISIS DETALLADO

### 1. API Gateway

#### application.yml (Perfil Local)

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9090/realms/videoclub
          jwk-set-uri: http://localhost:9090/realms/videoclub/protocol/openid-connect/certs

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

**✅ CORRECTO:**

- Issuer URI apunta a Keycloak local (localhost:9090)
- Ruta `ventas-carrito` correctamente definida
- StripPrefix=1 elimina `/api` → envía `/carrito/**` a Ventas
- URI apunta a Ventas en localhost:8083

#### SecurityConfig.java

```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers("/api/carrito/**").authenticated()  // ✅ CORRECTO
            .anyExchange().permitAll())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));  // ✅ CORRECTO
    return http.build();
}
```

**✅ CORRECTO:**

- Ruta `/api/carrito/**` requiere autenticación
- OAuth2 Resource Server configurado con JWT
- Sin JWT → Gateway debe devolver 401 Unauthorized

### 2. Servicio de Ventas

#### application-local.properties

```properties
server.port=8083
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9090/realms/videoclub
```

**✅ CORRECTO:**

- Puerto 8083 (mismo que espera el Gateway)
- Issuer URI correcto para Keycloak local
- **NO existe** `server.servlet.context-path` (bueno)
- **NO existe** `spring.mvc.servlet.path` (bueno)

#### CarritoController.java

```java
@RestController
@RequestMapping("/carrito")
public class CarritoController {

    @PostMapping("/items")
    public ResponseEntity<CarritoDTO> agregarPelicula(@RequestBody AgregarPeliculaRequest request) {
        // ...
    }
}
```

**✅ CORRECTO:**

- Endpoint mapeado en: `POST /carrito/items`
- Esto coincide con lo que el Gateway envía después de StripPrefix

#### SecurityConfig.java

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/carrito/**").authenticated()  // ✅ CORRECTO
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))  // ✅ CORRECTO
        .csrf(csrf -> csrf.disable());
    return http.build();
}
```

**✅ CORRECTO:**

- Ruta `/carrito/**` requiere autenticación
- OAuth2 Resource Server configurado con JWT
- Sin JWT → Ventas debe devolver 401 Unauthorized

---

## FLUJO ESPERADO

### Con JWT válido:

```
Cliente
  ↓ POST http://localhost:9500/api/carrito/items + Authorization: Bearer <JWT>
Gateway
  ↓ Valida JWT con Keycloak (issuer-uri)
  ↓ JWT válido → permite paso
  ↓ Aplica StripPrefix=1 (/api/carrito/items → /carrito/items)
  ↓ Forwarda a http://localhost:8083/carrito/items + Authorization: Bearer <JWT>
Ventas
  ↓ Recibe POST /carrito/items + JWT
  ↓ Valida JWT con Keycloak (issuer-uri)
  ↓ JWT válido → permite paso
  ↓ Ejecuta CarritoController.agregarPelicula()
  ↓ Devuelve 200 OK + CarritoDTO
Gateway
  ↓ Forwarda respuesta al cliente
Cliente
  ✅ Recibe 200 OK
```

### Sin JWT:

```
Cliente
  ↓ POST http://localhost:9500/api/carrito/items (sin Authorization)
Gateway
  ↓ SecurityWebFilterChain detecta que /api/carrito/** requiere authenticated()
  ↓ No hay JWT → rechaza request
  ✅ Devuelve 401 Unauthorized (NO llega a Ventas)
```

---

## DIAGNÓSTICO: ¿POR QUÉ 404?

Si estás obteniendo **404 Not Found**, las causas posibles son:

### A) Servicios no están corriendo

**Verificar:**

```powershell
# ¿Ventas está corriendo en 8083?
netstat -ano | findstr :8083

# ¿Gateway está corriendo en 9500?
netstat -ano | findstr :9500

# ¿Keycloak está corriendo en 9090?
netstat -ano | findstr :9090
```

**Si alguno NO está corriendo:**

- Arrancar el servicio faltante
- El 404 es porque el servicio no existe, no es problema de configuración

### B) Gateway no cargó las rutas

**Verificar:**

```powershell
# Consultar rutas cargadas
Invoke-RestMethod -Uri "http://localhost:9500/actuator/gateway/routes" | ConvertTo-Json -Depth 10
```

**Si `ventas-carrito` NO aparece:**

- Gateway no levantó las rutas del `application.yml`
- Posibles causas:
  - Error de sintaxis YAML (indentación incorrecta)
  - Perfil incorrecto activo (está usando `application-docker.yml` en lugar de `application.yml`)
  - Gateway falló al arrancar (revisar logs)

**Solución:**

- Verificar logs de arranque del Gateway
- Buscar líneas como:
  ```
  RouteDefinitionRouteLocator : Loaded RoutePredicateFactory [Path]
  RouteDefinitionRouteLocator : Loaded [ventas-carrito]
  ```
- Si no aparecen, hay un error en la carga de rutas

### C) Ventas no registró el mapping

**Verificar logs de arranque de Ventas:**

```
Tomcat started on port(s): 8083 (http)
RequestMappingHandlerMapping : Mapped "{[/carrito/items],methods=[POST]}" onto ...
```

**Si el mapping NO aparece:**

- El controller no fue escaneado por Spring
- Posible causa: `@ComponentScan` no incluye el paquete `unrn.api`
- Verificar que `ElAlmacenDePeliculasOnlineVentasApplication` tiene `@SpringBootApplication` (incluye `@ComponentScan`)

**Solución:**

- Si el problema es component scan, agregar explícitamente:
  ```java
  @SpringBootApplication
  @ComponentScan(basePackages = "unrn")
  public class ElAlmacenDePeliculasOnlineVentasApplication {
      // ...
  }
  ```

### D) Spring Security devuelve 404 en lugar de 401

**Esto ocurre si:**

- La ruta no está protegida en `SecurityConfig`
- Spring Security no está interceptando la ruta
- El filter chain no está aplicándose

**Verificar con logs de seguridad:**

```properties
# En application-local.properties, agregar:
logging.level.org.springframework.security=DEBUG
```

**Buscar en logs:**

```
FilterChainProxy : Securing POST /carrito/items
```

**Si NO aparece:**

- Spring Security no está activo
- Verificar que existe la dependencia `spring-boot-starter-oauth2-resource-server`

---

## RECOMENDACIONES

### 1. Habilitar Logs Detallados (Temporalmente)

**Gateway - application.yml:**

```yaml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
```

**Ventas - application-local.properties:**

```properties
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.unrn=DEBUG
```

### 2. Ejecutar Script de Prueba Automatizado

Ejecutar el script creado:

```powershell
cd "c:\Users\pelud\OneDrive\Documentos\UNRN\Taller de Tecnologías y Producción de Software\el-almacen-de-peliculas-online-ventas"

.\test-autenticacion-carrito.ps1
```

Este script:

- Obtiene un token de Keycloak automáticamente
- Prueba Ventas directo (con y sin token)
- Prueba Gateway (con y sin token)
- Verifica las rutas del Gateway
- Genera un reporte visual de éxito/fallo

### 3. Pruebas Manuales por Pasos

Seguir el documento `verificacion-keycloak-carrito.md` paso a paso:

1. Verificar Keycloak
2. Obtener token real
3. Probar Ventas directo
4. Probar Gateway
5. Capturar logs

---

## CONFIGURACIÓN PARA DOCKER

Si vas a ejecutar en Docker, asegurar que uses el perfil correcto:

### Gateway - application-docker.yml

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://keycloak:8080/realms/videoclub # ⚠️ Nombre del servicio docker
  cloud:
    gateway:
      routes:
        - id: ventas-carrito
          uri: http://ventas-service:8083 # ⚠️ Nombre del servicio docker
```

### Ventas - application-docker.properties

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://keycloak:8080/realms/videoclub
```

**Importante:**

- En Docker, usar nombres de servicios del `docker-compose.yml`
- NO usar `localhost` ni `127.0.0.1`
- Keycloak: `http://keycloak:8080` (no `:9090`)

---

## CHECKLIST DE VERIFICACIÓN

- [ ] Keycloak corriendo en localhost:9090
- [ ] Ventas corriendo en localhost:8083
- [ ] Gateway corriendo en localhost:9500
- [ ] Realm `videoclub` existe en Keycloak
- [ ] Client configurado con Direct Access Grants
- [ ] Usuario de prueba existe
- [ ] Token se obtiene exitosamente
- [ ] Ventas directo con token → 200 OK
- [ ] Ventas directo sin token → 401 Unauthorized
- [ ] Gateway con token → 200 OK
- [ ] Gateway sin token → 401 Unauthorized
- [ ] Logs de Gateway muestran route match
- [ ] Logs de Ventas muestran request recibido

---

## ARCHIVOS GENERADOS

1. **verificacion-keycloak-carrito.md**
   - Guía completa de verificación
   - Comandos PowerShell y curl
   - Diagnóstico de problemas comunes
   - Evidencias a capturar

2. **test-autenticacion-carrito.ps1**
   - Script automatizado de pruebas
   - Obtiene token automáticamente
   - Ejecuta todos los tests
   - Genera reporte visual

---

## PRÓXIMOS PASOS

1. **Ejecutar script automatizado:**

   ```powershell
   .\test-autenticacion-carrito.ps1
   ```

2. **Si el script falla en obtener token:**
   - Verificar que Keycloak está corriendo
   - Ajustar parámetros del script (ClientId, Username, Password)

3. **Si Ventas directo falla:**
   - El problema está en Ventas, no en el Gateway
   - Revisar logs de Ventas
   - Verificar que el proceso está corriendo

4. **Si Gateway falla pero Ventas directo funciona:**
   - El problema está en el Gateway
   - Verificar rutas con `/actuator/gateway/routes`
   - Revisar logs del Gateway

5. **Capturar evidencias:**
   - Screenshots de Keycloak (realm, client)
   - Salida completa del script
   - Logs de Gateway y Ventas durante las pruebas

---

## CONCLUSIÓN

**La configuración de código es correcta.** El problema reportado (404) es muy probablemente:

- Servicios no están corriendo
- Servicios corriendo en puertos incorrectos
- Gateway no cargó las rutas (error de perfil o sintaxis YAML)
- Ventas no registró el controller (error de component scan)

**Acción recomendada:** Ejecutar el script de prueba automatizado para identificar exactamente dónde está el problema.
