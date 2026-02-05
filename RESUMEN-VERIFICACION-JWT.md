# Resumen Ejecutivo: Verificación JWT Keycloak - Carrito

**Fecha:** 2026-01-28  
**Estado:** Configuración correcta - Problema de runtime a investigar

---

## ✅ CONFIGURACIÓN ACTUAL: CORRECTA

He revisado exhaustivamente la configuración de Gateway, Ventas y Keycloak. **Todo el código y configuración es correcto**:

### Gateway (apigateway-main)

- ✅ Ruta `ventas-carrito` correctamente definida en `application.yml`
- ✅ StripPrefix=1 configurado (`/api/carrito/**` → `/carrito/**`)
- ✅ SecurityConfig protege `/api/carrito/**` con `.authenticated()`
- ✅ OAuth2 Resource Server JWT configurado
- ✅ issuer-uri apunta a Keycloak correcto (local y docker)

### Ventas (el-almacen-de-peliculas-online-ventas)

- ✅ Controller mapeado en `POST /carrito/items`
- ✅ SecurityConfig protege `/carrito/**` con `.authenticated()`
- ✅ OAuth2 Resource Server JWT configurado
- ✅ issuer-uri apunta a Keycloak correcto (local y docker)
- ✅ NO existe context-path ni servlet-path (bueno)

### Keycloak

- ✅ Token endpoint: `/realms/videoclub/protocol/openid-connect/token`
- ✅ issuer: `http://localhost:9090/realms/videoclub` (local)
- ✅ issuer: `http://keycloak:8080/realms/videoclub` (docker)

---

## 📋 COMPORTAMIENTO ESPERADO

| Escenario                  | Request                                                            | Esperado               |
| -------------------------- | ------------------------------------------------------------------ | ---------------------- |
| **Con JWT válido**         | `POST http://localhost:9500/api/carrito/items` + Bearer token      | ✅ 200 OK + CarritoDTO |
| **Sin JWT**                | `POST http://localhost:9500/api/carrito/items` (sin Authorization) | ✅ 401 Unauthorized    |
| **Ventas directo con JWT** | `POST http://localhost:8083/carrito/items` + Bearer token          | ✅ 200 OK + CarritoDTO |
| **Ventas directo sin JWT** | `POST http://localhost:8083/carrito/items` (sin Authorization)     | ✅ 401 Unauthorized    |

---

## 🔍 CAUSA PROBABLE DEL 404

El problema reportado (404 en POST /api/carrito/items) **NO es de configuración**. Posibles causas:

1. **Servicios no están corriendo:**
   - Keycloak no en puerto 9090
   - Ventas no en puerto 8083
   - Gateway no en puerto 9500

2. **Gateway no cargó las rutas:**
   - Perfil incorrecto activo (docker en lugar de local)
   - Error de sintaxis YAML no detectado
   - Fallo al arrancar (revisar logs)

3. **Ventas no registró el controller:**
   - Component scan no encontró `CarritoController`
   - Fallo al arrancar (revisar logs)

---

## 🚀 ARCHIVOS GENERADOS

He creado **4 documentos** para facilitar la verificación:

### 1. verificacion-keycloak-carrito.md

**Guía completa paso a paso**

- Verificación de Keycloak (realm, client, usuario)
- Comandos para obtener token real
- Pruebas de Ventas directo (con y sin token)
- Pruebas de Gateway (con y sin token)
- Análisis de logs
- Diagnóstico de problemas comunes
- Corrección de problemas

**Usar cuando:** Necesitas entender el proceso completo y diagnosticar problemas.

### 2. test-autenticacion-carrito.ps1

**Script automatizado de pruebas**

- Obtiene token automáticamente de Keycloak
- Ejecuta todos los tests (Ventas directo y Gateway)
- Verifica rutas del Gateway
- Genera reporte visual con colores
- Indica exactamente qué está fallando

**Usar cuando:** Quieres ejecutar todas las pruebas rápidamente.

```powershell
# Ejecutar con valores por defecto
.\test-autenticacion-carrito.ps1

# Ejecutar con parámetros personalizados
.\test-autenticacion-carrito.ps1 -ClientId "mi-client" -Username "usuario" -Password "clave"
```

### 3. ANALISIS-CONFIGURACION.md

**Análisis técnico detallado**

- Revisión de cada archivo de configuración
- Explicación del flujo completo
- Diagnóstico de causas del 404
- Recomendaciones de logs
- Configuración para Docker
- Checklist de verificación

**Usar cuando:** Necesitas entender por qué la configuración es correcta y qué puede estar fallando.

### 4. QUICK-REFERENCE.md

**Referencia rápida de comandos**

- Comandos PowerShell y curl
- Obtener token (una línea)
- Pruebas manuales
- Diagnóstico rápido
- Troubleshooting común
- Checklist rápido

**Usar cuando:** Necesitas comandos específicos sin leer documentación extensa.

---

## ⚡ INICIO RÁPIDO

### Opción 1: Script Automatizado (RECOMENDADO)

```powershell
cd "el-almacen-de-peliculas-online-ventas"
.\test-autenticacion-carrito.ps1
```

El script te dirá exactamente qué está fallando.

### Opción 2: Verificación Manual

1. Obtener token:

   ```powershell
   $token = (Invoke-RestMethod -Uri "http://localhost:9090/realms/videoclub/protocol/openid-connect/token" -Method POST -ContentType "application/x-www-form-urlencoded" -Body @{grant_type="password";client_id="videoclub-client";username="testuser";password="test123"}).access_token
   ```

2. Probar Ventas:

   ```powershell
   $body = '{"peliculaId":"1","titulo":"Matrix","precioUnitario":100,"cantidad":1}'
   Invoke-RestMethod -Uri "http://localhost:8083/carrito/items" -Method POST -Headers @{"Authorization"="Bearer $token";"Content-Type"="application/json"} -Body $body
   ```

3. Probar Gateway:
   ```powershell
   Invoke-RestMethod -Uri "http://localhost:9500/api/carrito/items" -Method POST -Headers @{"Authorization"="Bearer $token";"Content-Type"="application/json"} -Body $body
   ```

---

## 🎯 ACCIONES INMEDIATAS

### 1️⃣ Verificar que todos los servicios estén corriendo

```powershell
netstat -ano | findstr ":9090 :8083 :9500"
```

Debe mostrar 3 líneas (Keycloak, Ventas, Gateway).

### 2️⃣ Ejecutar script de prueba

```powershell
.\test-autenticacion-carrito.ps1
```

### 3️⃣ Revisar el output del script

El script te indicará exactamente qué test falló:

- ❌ Si falla "Obtener token" → problema con Keycloak
- ❌ Si falla "Ventas directo CON token" → problema en Ventas
- ❌ Si falla "Gateway CON token" pero Ventas OK → problema en Gateway

### 4️⃣ Si aún hay 404, capturar logs

Habilitar logs detallados (ver `ANALISIS-CONFIGURACION.md`) y capturar:

- Logs de arranque de Ventas
- Logs de arranque de Gateway
- Logs durante el request

---

## 📊 EVIDENCIAS A ENTREGAR

Una vez ejecutado el script y las pruebas manuales, generar:

1. **Output del script completo:**

   ```powershell
   .\test-autenticacion-carrito.ps1 > evidencia-pruebas.txt 2>&1
   ```

2. **Rutas del Gateway:**

   ```powershell
   Invoke-RestMethod -Uri "http://localhost:9500/actuator/gateway/routes" | ConvertTo-Json -Depth 10 > evidencia-routes.json
   ```

3. **Screenshots:**
   - Keycloak Admin Console (realm y client)
   - Postman/curl con request y respuesta

4. **Logs:**
   - Fragmento de logs de Ventas al arrancar
   - Fragmento de logs de Gateway al arrancar
   - Logs durante request (con logging.level.security=DEBUG)

---

## 🔐 CONFIGURACIÓN DE KEYCLOAK NECESARIA

Si aún no tienes Keycloak configurado, necesitas:

### Realm: videoclub

- Nombre: `videoclub`
- Enabled: ✅

### Client: videoclub-client (o tu client ID)

- Client ID: `videoclub-client`
- Client authentication: ❌ OFF (public client)
- Standard flow: ✅ ON
- Direct access grants: ✅ ON ⚠️ (solo para testing)
- Valid redirect URIs: `http://localhost:5173/*`

### Usuario: testuser (o tu usuario)

- Username: `testuser`
- Password: `test123`
- Temporary: ❌ OFF
- Email verified: ✅ ON (opcional)

---

## ⚠️ IMPORTANTE: NO MODIFICAR CONFIGURACIÓN

**NO realizar estos cambios** (romperían la seguridad):

- ❌ Deshabilitar Spring Security
- ❌ Permitir todas las rutas sin autenticación
- ❌ Crear perfil "local sin seguridad"
- ❌ Quitar oauth2ResourceServer

La configuración actual es correcta y segura. El problema es de runtime, no de configuración.

---

## 📞 SIGUIENTES PASOS

1. **Ejecutar script automatizado** para identificar el punto exacto de falla
2. **Si el script falla en obtener token:**
   - Verificar Keycloak corriendo y accesible
   - Verificar realm `videoclub` existe
   - Verificar client configurado con Direct Access Grants
   - Verificar usuario existe con credenciales correctas

3. **Si Ventas directo falla:**
   - Verificar Ventas corriendo en puerto 8083
   - Verificar logs de arranque muestran: `Mapped "{[/carrito/items]}"`
   - Verificar issuer-uri en application-local.properties

4. **Si Gateway falla pero Ventas directo funciona:**
   - Verificar Gateway corriendo en puerto 9500
   - Verificar rutas cargadas: `GET /actuator/gateway/routes`
   - Verificar logs de arranque muestran: `Loaded [ventas-carrito]`

---

## ✅ CONCLUSIÓN

La configuración de Gateway y Ventas es **correcta y segura**. No se requieren cambios en el código. El problema reportado (404) debe investigarse a nivel de runtime:

- ¿Servicios corriendo?
- ¿Puertos correctos?
- ¿Keycloak accesible?
- ¿Rutas cargadas?

Ejecuta el script automatizado para diagnosticar rápidamente.

---

## 📁 ESTRUCTURA DE ARCHIVOS

```
el-almacen-de-peliculas-online-ventas/
├── RESUMEN-VERIFICACION-JWT.md           ← Este archivo (resumen ejecutivo)
├── verificacion-keycloak-carrito.md      ← Guía completa paso a paso
├── test-autenticacion-carrito.ps1        ← Script automatizado
├── ANALISIS-CONFIGURACION.md             ← Análisis técnico detallado
└── QUICK-REFERENCE.md                    ← Referencia rápida de comandos
```

**Leer en este orden:**

1. Este archivo (resumen)
2. Ejecutar `test-autenticacion-carrito.ps1`
3. Si hay problemas, consultar `ANALISIS-CONFIGURACION.md`
4. Para comandos específicos, consultar `QUICK-REFERENCE.md`
5. Para proceso completo manual, seguir `verificacion-keycloak-carrito.md`
