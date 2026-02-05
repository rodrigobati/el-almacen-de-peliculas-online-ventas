# Script de Prueba de Autenticación JWT - Carrito
# Fecha: 2026-01-28
# Objetivo: Automatizar las pruebas de autenticación para POST /api/carrito/items

param(
    [string]$KeycloakUrl = "http://localhost:9090",
    [string]$Realm = "videoclub",
    [string]$ClientId = "videoclub-client",
    [string]$Username = "testuser",
    [string]$Password = "test123",
    [string]$VentasUrl = "http://localhost:8083",
    [string]$GatewayUrl = "http://localhost:9500"
)

Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  PRUEBA DE AUTENTICACIÓN JWT - CARRITO" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Función para imprimir resultados
function Write-Result {
    param(
        [string]$Test,
        [string]$Expected,
        [string]$Actual,
        [bool]$Success
    )
    
    $color = if ($Success) { "Green" } else { "Red" }
    $symbol = if ($Success) { "✅" } else { "❌" }
    
    Write-Host "$symbol $Test" -ForegroundColor $color
    Write-Host "   Esperado: $Expected" -ForegroundColor Gray
    Write-Host "   Obtenido: $Actual" -ForegroundColor Gray
    Write-Host ""
}

# JSON Body para las pruebas
$testBody = @{
    peliculaId = "1"
    titulo = "The Matrix"
    precioUnitario = 100.00
    cantidad = 1
} | ConvertTo-Json

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
Write-Host "  PASO 1: OBTENER TOKEN DE KEYCLOAK" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
Write-Host ""

Write-Host "Parámetros de configuración:" -ForegroundColor White
Write-Host "  Keycloak URL: $KeycloakUrl" -ForegroundColor Gray
Write-Host "  Realm: $Realm" -ForegroundColor Gray
Write-Host "  Client ID: $ClientId" -ForegroundColor Gray
Write-Host "  Username: $Username" -ForegroundColor Gray
Write-Host ""

try {
    $tokenUrl = "$KeycloakUrl/realms/$Realm/protocol/openid-connect/token"
    Write-Host "Solicitando token a: $tokenUrl" -ForegroundColor Gray
    
    $tokenResponse = Invoke-RestMethod -Uri $tokenUrl `
        -Method POST `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{
            grant_type = "password"
            client_id = $ClientId
            username = $Username
            password = $Password
        }
    
    $accessToken = $tokenResponse.access_token
    $tokenPreview = $accessToken.Substring(0, [Math]::Min(50, $accessToken.Length))
    
    Write-Host "✅ Token obtenido exitosamente" -ForegroundColor Green
    Write-Host "   Token (primeros 50 chars): $tokenPreview..." -ForegroundColor Gray
    Write-Host "   Expira en: $($tokenResponse.expires_in) segundos" -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "❌ ERROR al obtener token de Keycloak" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Posibles causas:" -ForegroundColor Yellow
    Write-Host "  1. Keycloak no está corriendo en $KeycloakUrl" -ForegroundColor Yellow
    Write-Host "  2. Realm '$Realm' no existe" -ForegroundColor Yellow
    Write-Host "  3. Client '$ClientId' no existe o no tiene Direct Access Grants habilitado" -ForegroundColor Yellow
    Write-Host "  4. Usuario/contraseña incorrectos" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
Write-Host "  PASO 2: PROBAR VENTAS DIRECTO" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
Write-Host ""

# Test 2.1: Ventas directo con token
Write-Host "[2.1] POST $VentasUrl/carrito/items CON TOKEN" -ForegroundColor White
try {
    $ventasResponse = Invoke-RestMethod -Uri "$VentasUrl/carrito/items" `
        -Method POST `
        -Headers @{
            "Authorization" = "Bearer $accessToken"
            "Content-Type" = "application/json"
        } `
        -Body $testBody
    
    Write-Result `
        -Test "Ventas directo CON token" `
        -Expected "200 OK" `
        -Actual "200 OK - Respuesta recibida" `
        -Success $true
    
    Write-Host "   Respuesta:" -ForegroundColor Gray
    Write-Host "   $($ventasResponse | ConvertTo-Json -Compress)" -ForegroundColor Gray
    Write-Host ""
} catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    $statusDescription = $_.Exception.Response.StatusDescription
    
    if ($statusCode -eq 404) {
        Write-Result `
            -Test "Ventas directo CON token" `
            -Expected "200 OK" `
            -Actual "404 Not Found" `
            -Success $false
        
        Write-Host "   ⚠️  PROBLEMA: Endpoint no encontrado" -ForegroundColor Red
        Write-Host "   Verificar:" -ForegroundColor Yellow
        Write-Host "     - Servicio Ventas está corriendo en puerto 8083" -ForegroundColor Yellow
        Write-Host "     - No existe server.servlet.context-path configurado" -ForegroundColor Yellow
        Write-Host "     - Controller CarritoController está correctamente mapeado" -ForegroundColor Yellow
        Write-Host ""
    } elseif ($statusCode -eq 401 -or $statusCode -eq 403) {
        Write-Result `
            -Test "Ventas directo CON token" `
            -Expected "200 OK" `
            -Actual "$statusCode $statusDescription" `
            -Success $false
        
        Write-Host "   ⚠️  PROBLEMA: Token rechazado" -ForegroundColor Red
        Write-Host "   Verificar:" -ForegroundColor Yellow
        Write-Host "     - issuer-uri en application-local.properties" -ForegroundColor Yellow
        Write-Host "     - Debe ser: http://localhost:9090/realms/videoclub" -ForegroundColor Yellow
        Write-Host ""
    } else {
        Write-Result `
            -Test "Ventas directo CON token" `
            -Expected "200 OK" `
            -Actual "$statusCode $statusDescription" `
            -Success $false
        
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host ""
    }
}

# Test 2.2: Ventas directo sin token
Write-Host "[2.2] POST $VentasUrl/carrito/items SIN TOKEN" -ForegroundColor White
try {
    $ventasNoAuthResponse = Invoke-RestMethod -Uri "$VentasUrl/carrito/items" `
        -Method POST `
        -Headers @{
            "Content-Type" = "application/json"
        } `
        -Body $testBody
    
    Write-Result `
        -Test "Ventas directo SIN token" `
        -Expected "401 Unauthorized" `
        -Actual "200 OK (NO DEBERÍA PERMITIR)" `
        -Success $false
    
    Write-Host "   ⚠️  PROBLEMA: Endpoint NO protegido" -ForegroundColor Red
    Write-Host "   Verificar SecurityConfig en Ventas" -ForegroundColor Yellow
    Write-Host ""
} catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    $statusDescription = $_.Exception.Response.StatusDescription
    
    if ($statusCode -eq 401 -or $statusCode -eq 403) {
        Write-Result `
            -Test "Ventas directo SIN token" `
            -Expected "401/403" `
            -Actual "$statusCode $statusDescription" `
            -Success $true
    } elseif ($statusCode -eq 404) {
        Write-Result `
            -Test "Ventas directo SIN token" `
            -Expected "401/403" `
            -Actual "404 Not Found" `
            -Success $false
        
        Write-Host "   ⚠️  PROBLEMA: Spring Security no intercepta la ruta" -ForegroundColor Red
        Write-Host "   Verificar:" -ForegroundColor Yellow
        Write-Host "     - SecurityConfig tiene .requestMatchers('/carrito/**').authenticated()" -ForegroundColor Yellow
        Write-Host "     - oauth2ResourceServer está configurado correctamente" -ForegroundColor Yellow
        Write-Host ""
    } else {
        Write-Result `
            -Test "Ventas directo SIN token" `
            -Expected "401/403" `
            -Actual "$statusCode $statusDescription" `
            -Success $false
    }
}

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
Write-Host "  PASO 3: PROBAR GATEWAY" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
Write-Host ""

# Test 3.1: Gateway con token
Write-Host "[3.1] POST $GatewayUrl/api/carrito/items CON TOKEN" -ForegroundColor White
try {
    $gatewayResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/carrito/items" `
        -Method POST `
        -Headers @{
            "Authorization" = "Bearer $accessToken"
            "Content-Type" = "application/json"
        } `
        -Body $testBody
    
    Write-Result `
        -Test "Gateway CON token" `
        -Expected "200 OK" `
        -Actual "200 OK - Respuesta recibida" `
        -Success $true
    
    Write-Host "   Respuesta:" -ForegroundColor Gray
    Write-Host "   $($gatewayResponse | ConvertTo-Json -Compress)" -ForegroundColor Gray
    Write-Host ""
} catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    $statusDescription = $_.Exception.Response.StatusDescription
    
    if ($statusCode -eq 404) {
        Write-Result `
            -Test "Gateway CON token" `
            -Expected "200 OK" `
            -Actual "404 Not Found" `
            -Success $false
        
        Write-Host "   ⚠️  PROBLEMA: Ruta no encontrada en Gateway" -ForegroundColor Red
        Write-Host "   Verificar:" -ForegroundColor Yellow
        Write-Host "     - Rutas del Gateway con: GET $GatewayUrl/actuator/gateway/routes" -ForegroundColor Yellow
        Write-Host "     - Debe existir ruta 'ventas-carrito' con predicado Path=/api/carrito/**" -ForegroundColor Yellow
        Write-Host ""
    } else {
        Write-Result `
            -Test "Gateway CON token" `
            -Expected "200 OK" `
            -Actual "$statusCode $statusDescription" `
            -Success $false
        
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host ""
    }
}

# Test 3.2: Gateway sin token
Write-Host "[3.2] POST $GatewayUrl/api/carrito/items SIN TOKEN" -ForegroundColor White
try {
    $gatewayNoAuthResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/carrito/items" `
        -Method POST `
        -Headers @{
            "Content-Type" = "application/json"
        } `
        -Body $testBody
    
    Write-Result `
        -Test "Gateway SIN token" `
        -Expected "401 Unauthorized" `
        -Actual "200 OK (NO DEBERÍA PERMITIR)" `
        -Success $false
    
    Write-Host "   ⚠️  PROBLEMA: Gateway NO protege la ruta" -ForegroundColor Red
    Write-Host "   Verificar SecurityConfig en Gateway" -ForegroundColor Yellow
    Write-Host ""
} catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    $statusDescription = $_.Exception.Response.StatusDescription
    
    if ($statusCode -eq 401 -or $statusCode -eq 403) {
        Write-Result `
            -Test "Gateway SIN token" `
            -Expected "401/403" `
            -Actual "$statusCode $statusDescription" `
            -Success $true
    } else {
        Write-Result `
            -Test "Gateway SIN token" `
            -Expected "401/403" `
            -Actual "$statusCode $statusDescription" `
            -Success $false
    }
}

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
Write-Host "  PASO 4: VERIFICAR RUTAS DEL GATEWAY" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
Write-Host ""

try {
    Write-Host "Consultando: $GatewayUrl/actuator/gateway/routes" -ForegroundColor Gray
    $routes = Invoke-RestMethod -Uri "$GatewayUrl/actuator/gateway/routes"
    
    $carritoRoute = $routes | Where-Object { $_.route_id -eq "ventas-carrito" }
    
    if ($carritoRoute) {
        Write-Host "✅ Ruta 'ventas-carrito' encontrada" -ForegroundColor Green
        Write-Host ""
        Write-Host "Configuración:" -ForegroundColor White
        Write-Host "  ID: $($carritoRoute.route_id)" -ForegroundColor Gray
        Write-Host "  URI: $($carritoRoute.uri)" -ForegroundColor Gray
        Write-Host "  Predicate: $($carritoRoute.predicate)" -ForegroundColor Gray
        Write-Host "  Filters: $($carritoRoute.filters -join ', ')" -ForegroundColor Gray
        Write-Host ""
    } else {
        Write-Host "❌ Ruta 'ventas-carrito' NO encontrada" -ForegroundColor Red
        Write-Host ""
        Write-Host "Rutas disponibles:" -ForegroundColor Yellow
        $routes | ForEach-Object {
            Write-Host "  - $($_.route_id): $($_.predicate)" -ForegroundColor Gray
        }
        Write-Host ""
    }
} catch {
    Write-Host "⚠️  No se pudo consultar las rutas del Gateway" -ForegroundColor Yellow
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host "   Posible causa: Actuator no habilitado o Gateway no está corriendo" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  FIN DE LAS PRUEBAS" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "Documentación completa en: verificacion-keycloak-carrito.md" -ForegroundColor White
Write-Host ""
