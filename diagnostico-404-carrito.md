# Diagnóstico 404 Carrito (Ventas)

**Fecha:** 2026-01-27  
**Síntoma:** Error 404 Not Found al intentar agregar películas al carrito desde el front-end

---

## 📋 Resumen del Síntoma

**Request:**

```
POST http://localhost:9500/api/clientes/patito/carrito/items
```

**Response:**

```json
{
  "timestamp": "2026-01-27T19:50:32.174+00:00",
  "path": "/api/clientes/patito/carrito/items",
  "status": 404,
  "error": "Not Found",
  "requestId": "63b2a483-14"
}
```

**Contexto:**

- Origin del front: `http://localhost:5173`
- API_BASE del front: `http://localhost:9500/api` (config.js)
- Puerto del gateway: `9500`
- Puerto de ventas: `8083` (configurado en application-docker.properties)

---

## 🔍 Hipótesis Evaluadas

| #   | Hipótesis                                         | Evidencia a Favor                                                                                                                             | Cómo Verificar                                                                                              | Resultado                                          |
| --- | ------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| 1   | **Gateway no tiene ruta configurada para ventas** | No se encontró ninguna ruta `/api/ventas/**` ni `/api/clientes/**` ni `/clientes/**` en application.yml ni application-docker.yml del gateway | Revisar `apigateway-main/src/main/resources/application*.yml` y buscar rutas que coincidan con `/clientes/` | ✅ **CONFIRMADO** - Esta es la causa raíz          |
| 2   | Endpoint no existe en el backend de ventas        | El CarritoController existe y tiene `@RequestMapping("/clientes/{clienteId}/carrito")` con `@PostMapping("/items")`                           | Revisar `CarritoController.java`                                                                            | ❌ Descartado - El endpoint existe                 |
| 3   | Servicio de ventas no está corriendo              | -                                                                                                                                             | Verificar con `curl http://localhost:8083/clientes/test/carrito`                                            | ⚠️ Por verificar                                   |
| 4   | Path duplicado (/api/api)                         | API_BASE ya incluye `/api` y el controller no tiene `/api` en su path                                                                         | Revisar concatenación en carrito.js                                                                         | ❌ Descartado - La concatenación es correcta       |
| 5   | Puerto incorrecto en el front                     | Front apunta a 9500 (gateway), ventas corre en 8083                                                                                           | Verificar config.js y application-docker.properties                                                         | ❌ Descartado - La arquitectura prevé usar gateway |

---

## 🔬 Verificaciones Realizadas

### 1. Backend de Ventas (Puerto 8083)

**Archivo:** `el-almacen-de-peliculas-online-ventas/src/main/java/unrn/api/CarritoController.java`

```java
@RestController
@RequestMapping("/clientes/{clienteId}/carrito")
public class CarritoController {

    @PostMapping("/items")
    public ResponseEntity<CarritoDTO> agregarPelicula(
            @PathVariable String clienteId,
            @RequestBody AgregarPeliculaRequest request) {
        // ...
    }
}
```

**Conclusión:** El endpoint **SÍ existe** en el backend y está correctamente mapeado a:

- `POST /clientes/{clienteId}/carrito/items`

**Puerto configurado:**

- application.properties: No define puerto (usa default 8080)
- application-docker.properties: `server.port=8083`

---

### 2. Front-End

**Archivo:** `el-almacen-de-peliculas-online-front-end/src/api/config.js`

```javascript
export const API_BASE =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:9500/api";
```

**Archivo:** `el-almacen-de-peliculas-online-front-end/src/api/carrito.js`

```javascript
const url = `${API_BASE}/clientes/${encodeURIComponent(clienteId)}/carrito/items`;
// Resultado: http://localhost:9500/api/clientes/patito/carrito/items
```

**Conclusión:** El front está construyendo la URL correctamente y apuntando al gateway (9500).

---

### 3. API Gateway (Puerto 9500)

**Archivo:** `apigateway-main/src/main/resources/application.yml`

**Rutas configuradas:**

```yaml
routes:
  - id: catalogo
    uri: http://localhost:8081
    predicates:
      - Path=/api/peliculas/**
    filters:
      - StripPrefix=1

  - id: rating
    uri: http://localhost:8082
    predicates:
      - Path=/api/ratings/**
    filters:
      - StripPrefix=1

  - id: keycloak
    uri: http://localhost:9090
    predicates:
      - Path=/auth/**,/realms/**
```

**❌ PROBLEMA DETECTADO:** No existe ninguna ruta configurada para:

- `/api/ventas/**`
- `/api/clientes/**`
- `/clientes/**`

**Archivo:** `apigateway-main/src/main/resources/application-docker.yml`

```yaml
routes:
  - id: catalogo
    uri: http://catalogo-backend:8080
    predicates:
      - Path=/api/peliculas/**

  - id: rating
    uri: http://rating-service:8080
    predicates:
      - Path=/api/ratings/**

  - id: keycloak
    uri: http://keycloak:8080
    predicates:
      - Path=/auth/**,/realms/**
```

**❌ PROBLEMA DETECTADO:** Tampoco existe configuración para ventas en el perfil docker.

---

## 🎯 Causa Raíz

### **CONFIRMADO: Gateway sin ruta configurada para el servicio de Ventas**

El API Gateway en el puerto `9500` **NO tiene configurada ninguna ruta** que redirija los requests con path `/api/clientes/**` hacia el servicio de ventas (puerto 8083).

**Flujo actual:**

```
Front-end (5173)
    ↓
POST http://localhost:9500/api/clientes/patito/carrito/items
    ↓
API Gateway (9500) busca ruta que coincida con /api/clientes/**
    ↓
❌ NO ENCUENTRA RUTA CONFIGURADA
    ↓
Retorna 404 Not Found
```

**Flujo esperado:**

```
Front-end (5173)
    ↓
POST http://localhost:9500/api/clientes/patito/carrito/items
    ↓
API Gateway (9500) encuentra ruta /api/clientes/** → ventas-service
    ↓
StripPrefix=1 elimina /api del path
    ↓
Forward a: http://localhost:8083/clientes/patito/carrito/items
    ↓
✅ CarritoController responde 200 OK
```

---

## 🔧 Plan Mínimo de Corrección

### Opción A: Agregar ruta en el Gateway (RECOMENDADA)

**Archivos a modificar:**

1. **`apigateway-main/src/main/resources/application.yml`**

   Agregar después de la ruta de rating:

   ```yaml
   - id: ventas
     uri: http://localhost:8083
     predicates:
       - Path=/api/ventas/**,/api/clientes/**
     filters:
       - StripPrefix=1
   ```

2. **`apigateway-main/src/main/resources/application-docker.yml`**

   Agregar después de la ruta de rating:

   ```yaml
   - id: ventas
     uri: http://ventas-service:8083
     predicates:
       - Path=/api/ventas/**,/api/clientes/**
     filters:
       - StripPrefix=1
   ```

**Explicación:**

- `Path=/api/clientes/**` captura todas las rutas que empiezan con `/api/clientes/`
- `StripPrefix=1` elimina el primer segmento (`/api`), dejando `/clientes/patito/carrito/items`
- `uri: http://localhost:8083` (desarrollo) o `http://ventas-service:8083` (docker) redirecciona al servicio de ventas

### Opción B: Bypass del Gateway (NO RECOMENDADA - Solo para testing)

**Archivo a modificar:**

`el-almacen-de-peliculas-online-front-end/src/api/config.js`

```javascript
export const API_BASE = "http://localhost:8083";
```

**⚠️ Advertencia:** Esto rompe la arquitectura de microservicios y bypasea:

- CORS configurado en el gateway
- Autenticación centralizada
- Enrutamiento unificado

---

## ✅ Checklist de Pruebas Post-Fix

### 1. Verificar que el servicio de ventas está corriendo

```bash
# Verificar que el servicio responde directamente
curl http://localhost:8083/clientes/test/carrito

# Debe retornar 200 OK con JSON:
# {"items":[],"total":0}
```

### 2. Reiniciar el API Gateway

Después de modificar los archivos de configuración:

```bash
cd apigateway-main
./mvnw spring-boot:run
# O si ya está corriendo, detener y volver a iniciar
```

Verificar en los logs que se cargó la nueva ruta:

```
Mapped [/api/clientes/**] onto Route[id='ventas', ...]
```

### 3. Probar la ruta a través del gateway con curl

```bash
# Ver carrito (GET)
curl http://localhost:9500/api/clientes/test/carrito

# Debe retornar 200 OK con JSON: {"items":[],"total":0}

# Agregar película al carrito (POST)
curl -X POST http://localhost:9500/api/clientes/test/carrito/items \
  -H "Content-Type: application/json" \
  -d '{
    "peliculaId": "pel-001",
    "titulo": "Inception",
    "precioUnitario": 15.99,
    "cantidad": 1
  }'

# Debe retornar 200 OK con el carrito actualizado
```

### 4. Probar desde el front-end (UI)

1. Abrir `http://localhost:5173`
2. Hacer login (si es necesario)
3. Seleccionar una película
4. Click en "🛒 Agregar al carrito"
5. Verificar:
   - ✅ NO aparece error 404
   - ✅ Aparece mensaje "✅ Agregado al carrito"
   - ✅ En DevTools Network tab: `POST /api/clientes/.../carrito/items` retorna 200

### 5. Verificar logs del gateway

Durante las pruebas, monitorear los logs del gateway para confirmar que:

- El request llega al gateway
- Se matchea con la ruta `ventas`
- Se hace forward a `http://localhost:8083` (o `ventas-service:8083`)
- Se retorna la respuesta del servicio

---

## 📊 Diagrama de la Arquitectura

### Antes (con error 404)

```
┌─────────────┐
│   Front     │
│  (5173)     │
└──────┬──────┘
       │ POST /api/clientes/patito/carrito/items
       ↓
┌─────────────┐
│  Gateway    │
│  (9500)     │ ❌ No encuentra ruta
└─────────────┘
       ↓
    404 Not Found
```

### Después (con fix)

```
┌─────────────┐
│   Front     │
│  (5173)     │
└──────┬──────┘
       │ POST /api/clientes/patito/carrito/items
       ↓
┌─────────────┐
│  Gateway    │────┐ Matchea /api/clientes/**
│  (9500)     │    │ StripPrefix=1 → /clientes/...
└─────────────┘    │
                   │ Forward
                   ↓
            ┌──────────────┐
            │   Ventas     │
            │   (8083)     │
            └──────────────┘
                   ↓
            200 OK + JSON
```

---

## 🔍 Comandos de Diagnóstico Adicionales

### Verificar que el gateway está corriendo

```bash
curl http://localhost:9500/actuator/health
# Si actuator está habilitado, debe retornar: {"status":"UP"}
```

### Listar todas las rutas del gateway

```bash
curl http://localhost:9500/actuator/gateway/routes
# Requiere actuator con gateway endpoints expuestos
```

### Verificar puerto del servicio de ventas

```bash
# Windows
netstat -ano | findstr :8083

# Debe mostrar algo como:
# TCP    0.0.0.0:8083    0.0.0.0:0    LISTENING    12345
```

---

## 📝 Notas Adicionales

1. **Orden de las rutas:** En Spring Cloud Gateway, el orden de las rutas importa. Si se agrega una ruta muy genérica como `Path=/**` antes de rutas específicas, puede capturar todos los requests. Asegurar que las rutas específicas estén antes de las genéricas.

2. **StripPrefix:** El filtro `StripPrefix=1` elimina **1 segmento** del path. Si el path es `/api/clientes/patito/carrito/items`, después de StripPrefix queda `/clientes/patito/carrito/items`, que coincide perfectamente con el `@RequestMapping` del CarritoController.

3. **CORS:** El gateway ya tiene CORS configurado para `http://localhost:5173`, por lo que no deberían haber problemas de CORS después del fix.

4. **Alternativa sin StripPrefix:** Si se prefiere no usar StripPrefix, el CarritoController debería tener `@RequestMapping("/api/clientes/{clienteId}/carrito")`, pero esto acopla el backend a la estructura del gateway. **No recomendado**.

5. **Seguridad:** Actualmente el gateway tiene configurado OAuth2/JWT pero no se ve aplicado a las rutas. Considerar agregar filtros de autenticación si es necesario.

---

## 🎯 Conclusión

**Causa raíz confirmada:** El API Gateway no tiene configurada ninguna ruta que redirija requests con path `/api/clientes/**` al servicio de ventas.

**Solución:** Agregar la configuración de ruta en `application.yml` y `application-docker.yml` del gateway según el Plan Mínimo de Corrección - Opción A.

**Impacto:** Bajo riesgo. Es un cambio de configuración que no afecta código existente.

**Prioridad:** Alta. Bloquea funcionalidad crítica del carrito de compras.

**Estimación:** 5 minutos para implementar + 10 minutos de testing = 15 minutos total.
