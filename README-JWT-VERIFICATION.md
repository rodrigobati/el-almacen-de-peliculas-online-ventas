# 🔐 Verificación de Autenticación JWT con Keycloak

Documentación completa para verificar y diagnosticar la autenticación JWT entre Gateway, Ventas y Keycloak.

---

## 📋 ÍNDICE

1. [Inicio Rápido](#-inicio-rápido)
2. [Estructura de Documentación](#-estructura-de-documentación)
3. [Estado de la Configuración](#-estado-de-la-configuración)
4. [Flujo de Autenticación](#-flujo-de-autenticación)
5. [Comandos Rápidos](#-comandos-rápidos)
6. [Troubleshooting](#-troubleshooting)

---

## 🚀 INICIO RÁPIDO

### Opción 1: Script Automatizado (RECOMENDADO)

```powershell
# Ejecutar desde la carpeta del proyecto Ventas
.\test-autenticacion-carrito.ps1
```

El script:

- ✅ Obtiene token de Keycloak automáticamente
- ✅ Prueba Ventas directo (con y sin token)
- ✅ Prueba Gateway (con y sin token)
- ✅ Verifica rutas del Gateway
- ✅ Genera reporte visual con diagnóstico

**Ajustar parámetros si es necesario:**

```powershell
.\test-autenticacion-carrito.ps1 `
  -ClientId "tu-client-id" `
  -Username "tu-usuario" `
  -Password "tu-password"
```

### Opción 2: Verificación Manual

Seguir la guía paso a paso: **[verificacion-keycloak-carrito.md](verificacion-keycloak-carrito.md)**

---

## 📚 ESTRUCTURA DE DOCUMENTACIÓN

### 🎯 Para Empezar

| Documento                       | Descripción                             | Usar cuando                      |
| ------------------------------- | --------------------------------------- | -------------------------------- |
| **README-JWT-VERIFICATION.md**  | Este archivo (punto de entrada)         | Primera vez o referencia general |
| **RESUMEN-VERIFICACION-JWT.md** | Resumen ejecutivo + acciones inmediatas | Quieres visión general rápida    |
| **QUICK-REFERENCE.md**          | Comandos rápidos (PowerShell + curl)    | Necesitas un comando específico  |

### 📖 Guías Detalladas

| Documento                            | Descripción                       | Usar cuando                            |
| ------------------------------------ | --------------------------------- | -------------------------------------- |
| **verificacion-keycloak-carrito.md** | Guía completa paso a paso         | Verificación manual completa           |
| **ANALISIS-CONFIGURACION.md**        | Análisis técnico de configuración | Entender por qué la config es correcta |
| **DIFF-CAMBIOS.md**                  | Cambios realizados (diff)         | Revisar qué se modificó                |

### 🛠️ Herramientas

| Archivo                            | Tipo              | Descripción                  |
| ---------------------------------- | ----------------- | ---------------------------- |
| **test-autenticacion-carrito.ps1** | Script PowerShell | Automatiza todas las pruebas |

---

## ✅ ESTADO DE LA CONFIGURACIÓN

### Análisis Realizado

He revisado exhaustivamente toda la configuración:

- ✅ **Gateway (apigateway-main)**
  - Ruta `ventas-carrito` correcta
  - StripPrefix=1 correcto
  - SecurityConfig correcto
  - JWT issuer-uri correcto

- ✅ **Ventas (el-almacen-de-peliculas-online-ventas)**
  - Controller mapeado correctamente
  - SecurityConfig correcto
  - JWT issuer-uri correcto
  - Sin context-path ni servlet-path

- ✅ **Keycloak**
  - Token endpoint correcto
  - Issuer URI correcto (local y docker)

### Cambios Realizados

**Modificaciones en código:** 1 archivo

- `application-local.properties`: Logs de seguridad mejorados (DEBUG)

**Documentación generada:** 7 archivos

- Guías, scripts y referencias

Ver detalles en: **[DIFF-CAMBIOS.md](DIFF-CAMBIOS.md)**

---

## 🔄 FLUJO DE AUTENTICACIÓN

### Flujo Completo: Cliente → Gateway → Ventas

```
1. Cliente solicita token a Keycloak
   POST http://localhost:9090/realms/videoclub/protocol/openid-connect/token
   Body: grant_type=password, client_id=..., username=..., password=...
   ↓
2. Keycloak devuelve JWT
   Response: { "access_token": "eyJ...", "token_type": "Bearer", ... }
   ↓
3. Cliente hace request al Gateway con JWT
   POST http://localhost:9500/api/carrito/items
   Header: Authorization: Bearer eyJ...
   ↓
4. Gateway valida JWT con Keycloak
   - Consulta JWK Set de Keycloak
   - Verifica firma, issuer, expiración
   ↓
5. Gateway aplica filtros
   - StripPrefix=1: /api/carrito/items → /carrito/items
   ↓
6. Gateway forwarda a Ventas
   POST http://localhost:8083/carrito/items
   Header: Authorization: Bearer eyJ...
   ↓
7. Ventas valida JWT con Keycloak
   - Consulta JWK Set de Keycloak
   - Verifica firma, issuer, expiración
   ↓
8. Ventas ejecuta lógica de negocio
   CarritoController.agregarPelicula(...)
   ↓
9. Ventas devuelve respuesta
   Response: 200 OK + CarritoDTO
   ↓
10. Gateway forwarda respuesta al Cliente
    ✅ Cliente recibe 200 OK
```

### Sin JWT (Esperado: 401)

```
1. Cliente hace request al Gateway SIN JWT
   POST http://localhost:9500/api/carrito/items
   (sin Authorization header)
   ↓
2. Gateway detecta falta de JWT
   SecurityWebFilterChain: /api/carrito/** requiere authenticated()
   ↓
3. Gateway rechaza request
   ❌ Response: 401 Unauthorized
   (NO llega a Ventas)
```

---

## ⚡ COMANDOS RÁPIDOS

### Verificar Servicios Corriendo

```powershell
# Todos los servicios (Keycloak, Ventas, Gateway)
netstat -ano | findstr ":9090 :8083 :9500"
```

### Obtener Token de Keycloak

```powershell
# Una línea
$token = (Invoke-RestMethod -Uri "http://localhost:9090/realms/videoclub/protocol/openid-connect/token" -Method POST -ContentType "application/x-www-form-urlencoded" -Body @{grant_type="password";client_id="videoclub-client";username="testuser";password="test123"}).access_token
```

### Probar Ventas Directo

```powershell
# CON token (esperado: 200 OK)
$body = '{"peliculaId":"1","titulo":"Matrix","precioUnitario":100,"cantidad":1}'
Invoke-RestMethod -Uri "http://localhost:8083/carrito/items" -Method POST -Headers @{"Authorization"="Bearer $token";"Content-Type"="application/json"} -Body $body

# SIN token (esperado: 401 Unauthorized)
Invoke-RestMethod -Uri "http://localhost:8083/carrito/items" -Method POST -Headers @{"Content-Type"="application/json"} -Body $body
```

### Probar Gateway

```powershell
# CON token (esperado: 200 OK)
Invoke-RestMethod -Uri "http://localhost:9500/api/carrito/items" -Method POST -Headers @{"Authorization"="Bearer $token";"Content-Type"="application/json"} -Body $body

# SIN token (esperado: 401 Unauthorized)
Invoke-RestMethod -Uri "http://localhost:9500/api/carrito/items" -Method POST -Headers @{"Content-Type"="application/json"} -Body $body
```

### Verificar Rutas del Gateway

```powershell
Invoke-RestMethod -Uri "http://localhost:9500/actuator/gateway/routes" | ConvertTo-Json -Depth 10
```

**Más comandos:** Ver **[QUICK-REFERENCE.md](QUICK-REFERENCE.md)**

---

## 🐛 TROUBLESHOOTING

### ❌ Error: 404 Not Found

**Síntoma:** POST devuelve 404 en lugar de 200 OK o 401

**Causas posibles:**

1. Servicio no está corriendo
2. Gateway no cargó las rutas
3. Ventas no registró el controller
4. Context-path o servlet-path configurado

**Solución:**

1. Verificar servicios corriendo: `netstat -ano | findstr ":9090 :8083 :9500"`
2. Verificar rutas Gateway: `GET http://localhost:9500/actuator/gateway/routes`
3. Revisar logs de arranque de Ventas (buscar "Mapped")
4. Ejecutar script: `.\test-autenticacion-carrito.ps1`

### ❌ Error: 401 Unauthorized (con token válido)

**Síntoma:** Request con JWT válido devuelve 401

**Causas posibles:**

1. issuer-uri incorrecto
2. Token expirado
3. JWK Set no accesible

**Solución:**

1. Verificar issuer-uri en properties: `http://localhost:9090/realms/videoclub`
2. Obtener token nuevo (expira en 5 minutos por defecto)
3. Verificar JWK accesible: `GET http://localhost:9090/realms/videoclub/protocol/openid-connect/certs`

### ❌ Error: Connection refused

**Síntoma:** No se puede conectar al servicio

**Causas posibles:**

1. Servicio no está corriendo
2. Puerto incorrecto
3. Firewall bloqueando

**Solución:**

1. Arrancar el servicio
2. Verificar puerto en properties
3. Verificar firewall/antivirus

### ❌ Error: invalid_grant (al obtener token)

**Síntoma:** Keycloak rechaza credenciales

**Causas posibles:**

1. Usuario/password incorrectos
2. Direct Access Grants no habilitado
3. Usuario deshabilitado

**Solución:**

1. Verificar credenciales en Keycloak Admin Console
2. Habilitar Direct Access Grants en client
3. Verificar usuario enabled

**Más troubleshooting:** Ver **[ANALISIS-CONFIGURACION.md](ANALISIS-CONFIGURACION.md#f-ajuste-de-seguridad-para-respuesta-correcta-sin-token)**

---

## 🎯 CHECKLIST DE VERIFICACIÓN

### Pre-requisitos

- [ ] Keycloak corriendo en puerto 9090
- [ ] Ventas corriendo en puerto 8083
- [ ] Gateway corriendo en puerto 9500
- [ ] Realm `videoclub` existe en Keycloak
- [ ] Client configurado (Direct Access Grants ON)
- [ ] Usuario de prueba existe

### Pruebas

- [ ] Token se obtiene exitosamente
- [ ] Ventas directo + token → 200 OK
- [ ] Ventas directo - token → 401 Unauthorized
- [ ] Gateway + token → 200 OK
- [ ] Gateway - token → 401 Unauthorized

### Evidencias

- [ ] Output del script de prueba
- [ ] Rutas del Gateway (`/actuator/gateway/routes`)
- [ ] Logs de Ventas (arranque + request)
- [ ] Logs de Gateway (arranque + request)
- [ ] Screenshots de Keycloak (realm + client)

---

## 📞 SOPORTE

### Si el script automatizado falla:

1. **Copiar el output completo:**

   ```powershell
   .\test-autenticacion-carrito.ps1 > output.txt 2>&1
   ```

2. **Capturar logs de servicios:**
   - Logs de arranque de Ventas
   - Logs de arranque de Gateway
   - Logs durante el request

3. **Verificar Keycloak:**
   - Screenshot del realm
   - Screenshot del client (settings)
   - Screenshot del usuario

4. **Consultar documentación:**
   - `ANALISIS-CONFIGURACION.md` para diagnóstico
   - `verificacion-keycloak-carrito.md` para proceso manual

---

## 📄 DOCUMENTOS RELACIONADOS

- **Configuración completa:** [verificacion-keycloak-carrito.md](verificacion-keycloak-carrito.md)
- **Análisis técnico:** [ANALISIS-CONFIGURACION.md](ANALISIS-CONFIGURACION.md)
- **Comandos rápidos:** [QUICK-REFERENCE.md](QUICK-REFERENCE.md)
- **Resumen ejecutivo:** [RESUMEN-VERIFICACION-JWT.md](RESUMEN-VERIFICACION-JWT.md)
- **Diff de cambios:** [DIFF-CAMBIOS.md](DIFF-CAMBIOS.md)

---

## ⚖️ NOTAS IMPORTANTES

### ⚠️ Direct Access Grants

El flujo `password grant` (Direct Access Grants) usado en estas pruebas es **SOLO para testing/desarrollo**.

**En producción usar:**

- Authorization Code Flow (OAuth2 standard)
- Client Credentials (para service-to-service)

### 🔒 Seguridad

**NO realizar estos cambios** (rompen seguridad):

- ❌ Deshabilitar Spring Security
- ❌ Permitir todas las rutas sin autenticación
- ❌ Crear perfil "sin seguridad"
- ❌ Quitar oauth2ResourceServer

La configuración actual es correcta y segura.

### 🐳 Docker vs Local

**Local (localhost):**

- Keycloak: `http://localhost:9090`
- Ventas: `http://localhost:8083`
- Gateway: `http://localhost:9500`

**Docker (nombres de servicios):**

- Keycloak: `http://keycloak:8080`
- Ventas: `http://ventas-service:8083`
- Gateway: `http://api-gateway:9500`

Asegurar usar el perfil correcto (`local` o `docker`).

---

## 📜 LICENCIA Y USO

Esta documentación es parte del proyecto **El Almacén de Películas Online** (Vertical Ventas).

**Autor:** Senior Engineer Spring Boot + Keycloak + Spring Cloud Gateway  
**Fecha:** 2026-01-28  
**Versión:** 1.0.0

---

**¿Empezar?** → Ejecuta: `.\test-autenticacion-carrito.ps1`
