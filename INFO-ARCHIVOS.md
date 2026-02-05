# Información de Archivos - Gateway y Ventas

## API Gateway - Archivos de Configuración

### Directorio

```
apigateway-main/src/main/resources/
```

### Archivos de Configuración

#### 1. application.yml (Local)

**Nombre:** `application.yml`  
**Path completo:**

```
c:/Users/pelud/OneDrive/Documentos/UNRN/Taller de Tecnologías y Producción de Software/apigateway-main/src/main/resources/application.yml
```

**Uso:** Configuración para desarrollo local (localhost:9090, localhost:8081, localhost:8082, localhost:8083)

#### 2. application-docker.yml (Docker)

**Nombre:** `application-docker.yml`  
**Path completo:**

```
c:/Users/pelud/OneDrive/Documentos/UNRN/Taller de Tecnologías y Producción de Software/apigateway-main/src/main/resources/application-docker.yml
```

**Uso:** Configuración para contenedores Docker (keycloak:8080, catalogo-backend:8080, rating-service:8080, ventas-service:8083)

---

## Ventas - Controller

### CarritoController

**Path completo:**

```
c:/Users/pelud/OneDrive/Documentos/UNRN/Taller de Tecnologías y Producción de Software/el-almacen-de-peliculas-online-ventas/src/main/java/unrn/api/CarritoController.java
```

**Package:** `unrn.api`

**RequestMapping:** `@RequestMapping("/carrito")`

### Endpoints Expuestos

| Método | Path                          | Descripción                         |
| ------ | ----------------------------- | ----------------------------------- |
| GET    | `/carrito`                    | Ver carrito del usuario autenticado |
| POST   | `/carrito/items`              | Agregar película al carrito         |
| DELETE | `/carrito/items/{peliculaId}` | Eliminar película del carrito       |

### Estructura de Paquetes

```
el-almacen-de-peliculas-online-ventas/
└── src/
    └── main/
        └── java/
            └── unrn/
                ├── api/
                │   └── CarritoController.java
                ├── dto/
                ├── model/
                ├── repository/
                ├── security/
                │   └── SecurityConfig.java
                └── service/
                    └── CarritoService.java
```

---

## Notas Importantes

### Gateway

- **Puerto:** 9500
- **Perfiles:** Por defecto usa `application.yml`, con `SPRING_PROFILES_ACTIVE=docker` usa `application-docker.yml`

### Ventas

- **Puerto:** 8083
- **Perfiles:**
  - `local` → `application-local.properties`
  - `docker` → `application-docker.properties`
- **Autenticación:** JWT con Keycloak (claim `preferred_username`)
