# Diff de Cambios Realizados

**Fecha:** 2026-01-28  
**Objetivo:** Mejorar logs de diagnóstico para JWT

---

## CAMBIOS REALIZADOS

### 1. Mejora de Logs en Ventas

**Archivo:** `el-almacen-de-peliculas-online-ventas/src/main/resources/application-local.properties`

**Cambio:** Agregados logs de Spring Security para facilitar diagnóstico.

```diff
# ========================================
# LOGGING
# ========================================
logging.level.root=INFO
logging.level.unrn=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.boot.web=DEBUG
+ logging.level.org.springframework.security=DEBUG
+ logging.level.org.springframework.security.web=DEBUG
+ logging.level.org.springframework.security.oauth2=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n
```

**Justificación:**

- Permite ver el flujo completo de autenticación JWT
- Facilita diagnóstico de problemas de issuer-uri o JWK
- Muestra si Spring Security está interceptando las rutas correctamente
- Identifica si el token es válido o por qué fue rechazado

**Logs adicionales visibles:**

- `JwtAuthenticationProvider : Authenticated: Principal: ...` (token válido)
- `BearerTokenAuthenticationFilter : Did not process request since did not find bearer token` (sin token)
- `FilterChainProxy : Secured POST /carrito/items` (ruta protegida)
- `NimbusJwtDecoder : Failed to validate the token` (token inválido o expirado)

---

## ARCHIVOS CREADOS (NO MODIFICAN CÓDIGO PRODUCTIVO)

### 1. verificacion-keycloak-carrito.md

Guía completa paso a paso con todos los comandos necesarios para verificar el flujo JWT.

### 2. test-autenticacion-carrito.ps1

Script automatizado que ejecuta todas las pruebas y genera un reporte visual.

### 3. ANALISIS-CONFIGURACION.md

Análisis técnico detallado de la configuración actual (Gateway + Ventas + Keycloak).

### 4. QUICK-REFERENCE.md

Referencia rápida de comandos PowerShell y curl para pruebas manuales.

### 5. RESUMEN-VERIFICACION-JWT.md

Resumen ejecutivo con inicio rápido y estructura de archivos.

### 6. DIFF-CAMBIOS.md

Este archivo, documentando los cambios realizados.

---

## ARCHIVOS NO MODIFICADOS (CONFIGURACIÓN CORRECTA)

Los siguientes archivos fueron revisados y **NO requieren cambios**:

### Gateway (apigateway-main)

**application.yml** - ✅ CORRECTO

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9090/realms/videoclub
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

**SecurityConfig.java** - ✅ CORRECTO

```java
.pathMatchers("/api/carrito/**").authenticated()
.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
```

### Ventas (el-almacen-de-peliculas-online-ventas)

**application-local.properties** - ✅ CORRECTO (con mejora de logs)

```properties
server.port=8083
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9090/realms/videoclub
```

**SecurityConfig.java** - ✅ CORRECTO

```java
.requestMatchers("/carrito/**").authenticated()
.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
```

**CarritoController.java** - ✅ CORRECTO

```java
@RestController
@RequestMapping("/carrito")
public class CarritoController {
    @PostMapping("/items")
    public ResponseEntity<CarritoDTO> agregarPelicula(@RequestBody AgregarPeliculaRequest request)
}
```

---

## RESUMEN DE CAMBIOS

| Archivo                            | Tipo de Cambio | Impacto                                       |
| ---------------------------------- | -------------- | --------------------------------------------- |
| `application-local.properties`     | Mejora de logs | Facilita diagnóstico, no cambia funcionalidad |
| `verificacion-keycloak-carrito.md` | Nuevo          | Documentación                                 |
| `test-autenticacion-carrito.ps1`   | Nuevo          | Script de pruebas                             |
| `ANALISIS-CONFIGURACION.md`        | Nuevo          | Documentación                                 |
| `QUICK-REFERENCE.md`               | Nuevo          | Documentación                                 |
| `RESUMEN-VERIFICACION-JWT.md`      | Nuevo          | Documentación                                 |
| `DIFF-CAMBIOS.md`                  | Nuevo          | Este archivo                                  |

**Total de cambios en código productivo:** 1 archivo (solo logs)  
**Total de archivos de documentación:** 6 archivos

---

## VALIDACIÓN DE CAMBIOS

### Antes del cambio:

```properties
logging.level.org.springframework.security=INFO  # (default)
```

**Problema:** Logs de seguridad no visibles, difícil diagnosticar problemas de JWT.

### Después del cambio:

```properties
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.security.web=DEBUG
logging.level.org.springframework.security.oauth2=DEBUG
```

**Mejora:** Logs de seguridad visibles para diagnóstico de JWT, sin afectar funcionalidad.

---

## IMPACTO EN ENTORNOS

### Entorno Local (application-local.properties)

- ✅ Logs de seguridad habilitados (DEBUG)
- ℹ️ Puede generar más logs, pero facilita desarrollo y diagnóstico

### Entorno Docker (application-docker.properties)

- ✅ Sin cambios
- ℹ️ Logs permanecen en INFO (producción-like)

### Entorno de Producción

- ✅ Sin cambios
- ✅ El perfil `local` no se usa en producción
- ✅ Logs de seguridad en DEBUG solo afectan desarrollo local

---

## PRÓXIMOS PASOS

1. **Ejecutar script de prueba:**

   ```powershell
   .\test-autenticacion-carrito.ps1
   ```

2. **Verificar logs mejorados:**
   - Al arrancar Ventas, los logs mostrarán más detalles de seguridad
   - Durante requests, se verá el flujo completo de autenticación JWT

3. **Si aún hay 404:**
   - Los logs de seguridad ayudarán a identificar si el problema es:
     - Spring Security no interceptando la ruta
     - Token siendo rechazado por algún motivo
     - Ruta no protegida correctamente

4. **Capturar evidencias:**
   - Logs de arranque de Ventas (con mappings de endpoints)
   - Logs durante request (con flujo de autenticación)
   - Output del script de prueba

---

## ROLLBACK (si es necesario)

Si por algún motivo necesitas revertir los cambios de logs:

```diff
# application-local.properties
logging.level.root=INFO
logging.level.unrn=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.boot.web=DEBUG
- logging.level.org.springframework.security=DEBUG
- logging.level.org.springframework.security.web=DEBUG
- logging.level.org.springframework.security.oauth2=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n
```

**Nota:** No recomendado, ya que los logs adicionales facilitan el diagnóstico sin impacto funcional.

---

## CONCLUSIÓN

**Cambios mínimos implementados:**

- ✅ Solo se modificó 1 archivo (`application-local.properties`)
- ✅ Cambio no afecta funcionalidad, solo visibilidad de logs
- ✅ Cambio solo aplica en entorno local (desarrollo)
- ✅ Documentación completa generada para facilitar verificación

**Configuración validada:**

- ✅ Gateway correctamente configurado
- ✅ Ventas correctamente configurado
- ✅ Seguridad JWT correctamente implementada
- ✅ Rutas correctamente protegidas

**Próxima acción:**
Ejecutar el script `test-autenticacion-carrito.ps1` para identificar el punto exacto de falla (runtime).
