# Quick Reference: Comandos de Verificación JWT

## 🚀 INICIO RÁPIDO

### 1. Ejecutar Script Automatizado (RECOMENDADO)

```powershell
cd "c:\Users\pelud\OneDrive\Documentos\UNRN\Taller de Tecnologías y Producción de Software\el-almacen-de-peliculas-online-ventas"
.\test-autenticacion-carrito.ps1
```

Si necesitas ajustar parámetros:

```powershell
.\test-autenticacion-carrito.ps1 `
  -ClientId "tu-client-id" `
  -Username "tu-usuario" `
  -Password "tu-password"
```

---

## 🔍 VERIFICACIONES MANUALES

### Verificar servicios corriendo

```powershell
# Keycloak en 9090
netstat -ano | findstr :9090

# Ventas en 8083
netstat -ano | findstr :8083

# Gateway en 9500
netstat -ano | findstr :9500
```

### Verificar rutas del Gateway

```powershell
Invoke-RestMethod -Uri "http://localhost:9500/actuator/gateway/routes" | ConvertTo-Json -Depth 10
```

### Probar Keycloak accesible

```powershell
# Verificar well-known config
Invoke-RestMethod -Uri "http://localhost:9090/realms/videoclub/.well-known/openid-configuration"

# Verificar JWK Set
Invoke-RestMethod -Uri "http://localhost:9090/realms/videoclub/protocol/openid-connect/certs"
```

---

## 🔐 OBTENER TOKEN

### PowerShell (una línea)

```powershell
$token = (Invoke-RestMethod -Uri "http://localhost:9090/realms/videoclub/protocol/openid-connect/token" -Method POST -ContentType "application/x-www-form-urlencoded" -Body @{grant_type="password";client_id="videoclub-client";username="testuser";password="test123"}).access_token
```

### PowerShell (completo)

```powershell
$response = Invoke-RestMethod `
  -Uri "http://localhost:9090/realms/videoclub/protocol/openid-connect/token" `
  -Method POST `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type = "password"
    client_id = "videoclub-client"
    username = "testuser"
    password = "test123"
  }

$token = $response.access_token
Write-Host "Token: $($token.Substring(0,50))..."
```

### curl (Git Bash)

```bash
export TOKEN=$(curl -s -X POST "http://localhost:9090/realms/videoclub/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=videoclub-client" \
  -d "username=testuser" \
  -d "password=test123" | jq -r '.access_token')

echo "Token: ${TOKEN:0:50}..."
```

---

## 🧪 PRUEBAS CON TOKEN

### Ventas Directo - CON Token

```powershell
$body = '{"peliculaId":"1","titulo":"The Matrix","precioUnitario":100,"cantidad":1}'
Invoke-RestMethod -Uri "http://localhost:8083/carrito/items" `
  -Method POST `
  -Headers @{"Authorization"="Bearer $token";"Content-Type"="application/json"} `
  -Body $body
```

### Ventas Directo - SIN Token (debe dar 401)

```powershell
Invoke-RestMethod -Uri "http://localhost:8083/carrito/items" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body $body
```

### Gateway - CON Token

```powershell
Invoke-RestMethod -Uri "http://localhost:9500/api/carrito/items" `
  -Method POST `
  -Headers @{"Authorization"="Bearer $token";"Content-Type"="application/json"} `
  -Body $body
```

### Gateway - SIN Token (debe dar 401)

```powershell
Invoke-RestMethod -Uri "http://localhost:9500/api/carrito/items" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body $body
```

---

## 🐛 DIAGNÓSTICO RÁPIDO

### Ver logs de Spring Security (agregar en application properties)

```properties
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web=DEBUG
```

### Inspeccionar JWT (online)

1. Copiar el token
2. Ir a: https://jwt.io
3. Pegar en "Encoded"
4. Verificar:
   - `iss`: debe ser `http://localhost:9090/realms/videoclub`
   - `exp`: debe ser timestamp futuro (no expirado)
   - `sub`: debe tener el usuario

### Inspeccionar JWT (PowerShell)

```powershell
# Decodificar payload del JWT (sin verificar firma)
$payload = $token.Split('.')[1]
$padded = $payload + ('=' * ((4 - ($payload.Length % 4)) % 4))
$decoded = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($padded))
$decoded | ConvertFrom-Json | ConvertTo-Json
```

---

## 📊 RESPUESTAS ESPERADAS

| Test            | Esperado              | Si falla                                         |
| --------------- | --------------------- | ------------------------------------------------ |
| Obtener token   | 200 OK + access_token | Keycloak no corriendo / credenciales incorrectas |
| Ventas + token  | 200 OK + CarritoDTO   | issuer-uri incorrecto / endpoint no mapeado      |
| Ventas - token  | 401 Unauthorized      | SecurityConfig mal configurado                   |
| Gateway + token | 200 OK + CarritoDTO   | Ruta no cargada / Ventas no accesible            |
| Gateway - token | 401 Unauthorized      | SecurityConfig mal configurado                   |

---

## 🔧 TROUBLESHOOTING

### Error: "Connection refused" en token

❌ Keycloak no está corriendo en puerto 9090
✅ Arrancar Keycloak o verificar puerto

### Error: "invalid_grant" al obtener token

❌ Usuario/contraseña incorrectos O Direct Access Grants no habilitado
✅ Verificar credenciales en Keycloak Admin Console

### Error: 404 en Ventas directo

❌ Servicio no corriendo O endpoint no mapeado
✅ Verificar logs de arranque de Ventas (`Mapped "{[/carrito/items]}"`)

### Error: 401 en Ventas con token válido

❌ issuer-uri incorrecto en Ventas
✅ Debe ser: `http://localhost:9090/realms/videoclub`

### Error: 404 en Gateway pero Ventas directo funciona

❌ Gateway no cargó la ruta `ventas-carrito`
✅ Verificar: `GET http://localhost:9500/actuator/gateway/routes`

---

## 📚 DOCUMENTACIÓN COMPLETA

- **verificacion-keycloak-carrito.md**: Guía paso a paso detallada
- **ANALISIS-CONFIGURACION.md**: Análisis de configuración y diagnóstico
- **test-autenticacion-carrito.ps1**: Script automatizado de pruebas

---

## 🎯 CHECKLIST RÁPIDO

```
[ ] Keycloak corriendo (puerto 9090)
[ ] Ventas corriendo (puerto 8083)
[ ] Gateway corriendo (puerto 9500)
[ ] Token obtenido exitosamente
[ ] Ventas directo + token → 200 OK
[ ] Ventas directo - token → 401
[ ] Gateway + token → 200 OK
[ ] Gateway - token → 401
```

---

## 💡 TIP: Configuración de Keycloak para Testing

Si aún no tienes el realm configurado:

1. **Crear Realm:**
   - Ir a Keycloak Admin: http://localhost:9090/admin
   - Login: admin / admin
   - Click "Create realm" → Name: `videoclub`

2. **Crear Client:**
   - Ir a Clients → Create client
   - Client ID: `videoclub-client`
   - Client authentication: OFF (public client)
   - Standard flow: ON
   - Direct access grants: ON ⚠️ (para testing)
   - Valid redirect URIs: `http://localhost:5173/*`

3. **Crear Usuario:**
   - Ir a Users → Create user
   - Username: `testuser`
   - Ir a Credentials → Set password
   - Password: `test123`
   - Temporary: OFF

4. **Verificar:**
   - Obtener token con script
   - Debe devolver access_token
