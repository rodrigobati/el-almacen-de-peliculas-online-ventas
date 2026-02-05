# Diagnostico 500 POST /api/carrito/items (Gateway -> Ventas)

Fecha: 2026-02-05

## 1) Resumen del problema

- El front realiza `POST http://localhost:9500/api/carrito/items` y recibe **500**.
- El mismo request directo a Ventas (`http://localhost:8083/carrito/items`) devuelve **200**.

## 2) Evidencia del request (front)

**Origen (front):**

- API base: `http://localhost:9500/api`
- Endpoint: `/carrito/items`
- Archivo: el envio se realiza desde `src/api/carrito.js`.

**Request observado (equivalente al front):**

- URL: `http://localhost:9500/api/carrito/items`
- Method: `POST`
- Headers:
  - `Authorization: Bearer <TOKEN>`
  - `Content-Type: application/json`
- Body:

```json
{
  "peliculaId": "1",
  "titulo": "The Matrix",
  "precioUnitario": 100.0,
  "cantidad": 1
}
```

## 3) Routing exacto en el Gateway

| Entrada Gateway      | Filters         | Salida a Ventas  |
| -------------------- | --------------- | ---------------- |
| `/api/carrito/items` | `StripPrefix=1` | `/carrito/items` |

## 4) Donde se genera el 500 (evidencia)

**Resultado de reproduccion:**

- Gateway: `POST http://localhost:9500/api/carrito/items` -> **500**
- Ventas directo: `POST http://localhost:8083/carrito/items` -> **200**

**Log del Gateway (error real):**

```
2026-02-05 17:38:03 - o.s.b.a.w.r.e.AbstractErrorWebExceptionHandler - 500 Server Error for HTTP POST "/api/carrito/items"
java.lang.IllegalStateException: Could not obtain the keys
Caused by: org.springframework.web.reactive.function.client.WebClientRequestException: Failed to resolve 'keycloak'
Caused by: java.net.UnknownHostException: Failed to resolve 'keycloak'
```

**Log de Ventas (request OK):**

```
2026-02-05 20:41:23 - ... RequestResponseBodyMethodProcessor - Read "application/json;charset=UTF-8" to [AgregarPeliculaRequest[peliculaId=1, titulo=The Matrix, precioUnitario=100, cantidad=1]]
2026-02-05 20:41:23 - ... HttpEntityMethodProcessor - Writing [CarritoDTO[items=[PeliculaEnCarritoDTO[peliculaId=1, titulo=The Matrix, precioUnitario=100, cantidad=2, subtotal=200]], total=200]]
```

## 5) Causa raiz exacta

El 500 lo genera el **API Gateway** durante la validacion JWT, antes de rutear a Ventas. El `ReactiveJwtDecoder` intenta leer el JWK en `http://keycloak:8080/...` y falla porque el hostname `keycloak` no existe en la red actual (el contenedor de Keycloak se llama `keycloak-sso`).

## 6) Solucion aplicada (minima y correcta)

### Antes

- El Gateway tenia el `jwk-set-uri` hardcodeado a `http://keycloak:8080/...` en el `SecurityConfig`.
- El profile docker tambien usaba `keycloak` en `application-docker.yml`.

### Despues

- El `SecurityConfig` ahora usa el `jwk-set-uri` configurado por properties.
- En docker, se apunta a `keycloak-sso`.

## 7) Cambios realizados

- [apigateway-main/src/main/java/com/videoclub/apigateway/config/SecurityConfig.java](apigateway-main/src/main/java/com/videoclub/apigateway/config/SecurityConfig.java)
- [apigateway-main/src/main/resources/application-docker.yml](apigateway-main/src/main/resources/application-docker.yml)

## 8) Comandos de verificacion

**PowerShell (Gateway):**

```powershell
$KEYCLOAK_URL = "http://localhost:9090"
$REALM = "videoclub"
$CLIENT_ID = "web"
$USERNAME = "patito"
$PASSWORD = "patito"
$tokenResponse = Invoke-RestMethod -Uri "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" -Method POST -ContentType "application/x-www-form-urlencoded" -Body @{ grant_type = "password"; client_id = $CLIENT_ID; username = $USERNAME; password = $PASSWORD }
$ACCESS_TOKEN = $tokenResponse.access_token

$body = @{ peliculaId = "1"; titulo = "The Matrix"; precioUnitario = 100.00; cantidad = 1 } | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:9500/api/carrito/items" -Method POST -Headers @{ Authorization = "Bearer $ACCESS_TOKEN"; "Content-Type" = "application/json" } -Body $body
```

**PowerShell (Ventas directo):**

```powershell
Invoke-WebRequest -Uri "http://localhost:8083/carrito/items" -Method POST -Headers @{ Authorization = "Bearer $ACCESS_TOKEN"; "Content-Type" = "application/json" } -Body $body
```

## 9) Estado final

- `POST /api/carrito/items` devuelve **200** desde el Gateway.
- El usuario deja de ver **500** en el front.
- La arquitectura se mantiene: Front -> Gateway -> Ventas.
