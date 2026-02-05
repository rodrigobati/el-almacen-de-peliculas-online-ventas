# Vertical de Ventas

## Descripción general

La vertical de Ventas se encarga de gestionar el proceso de compra de películas en el sistema El Almacén de Películas Online. Su responsabilidad es permitir que los clientes administren su carrito de compras y completen transacciones de manera consistente y segura.

Esta vertical opera sobre el carrito de compras del cliente, proporcionando las capacidades necesarias para agregar, modificar y eliminar películas antes de confirmar una venta. Se integra conceptualmente con otras verticales del sistema:

- **Catálogo**: consume información de películas disponibles (título, precio).
- **Usuarios**: asocia carritos y ventas a clientes identificados.
- **Pagos** (fuera de alcance): delega el procesamiento de pagos externos.

## Alcance funcional

La vertical de Ventas cubre las siguientes responsabilidades:

- **Gestión del carrito de compras**: agregar películas, ajustar cantidades, eliminar ítems.
- **Cálculo de totales**: determinar el subtotal por película y el total del carrito.
- **Visualización del carrito**: mostrar las películas seleccionadas con su información relevante (título, precio, cantidad).
- **Inicio de una venta**: transición del carrito a un proceso de compra confirmado.
- **Registro de ventas**: persistir la información de la venta una vez confirmada.

## Casos de uso principales

1. **Agregar película al carrito**  
   El cliente selecciona una película del catálogo y la agrega a su carrito. Si la película ya está en el carrito, se incrementa la cantidad.

2. **Eliminar película del carrito**  
   El cliente puede remover una película completa de su carrito antes de proceder a la compra.

3. **Ver detalle del carrito**  
   El cliente visualiza todas las películas en su carrito, incluyendo título, precio unitario, cantidad y subtotal por ítem.

4. **Calcular total del carrito**  
   El sistema calcula automáticamente el total a pagar sumando los subtotales de todas las películas.

5. **Confirmar compra**  
   El cliente finaliza el proceso de compra, convirtiendo el carrito en una venta registrada.

## Modelo de dominio involucrado

### Agregados y entidades

- **Carrito** (agregado raíz)  
  Representa el carrito de compras de un cliente. Mantiene la colección de películas seleccionadas y coordina las operaciones de agregar, eliminar y calcular totales.

- **PeliculaEnCarrito** (entidad del agregado)  
  Representa una película en el contexto del carrito de compras. Incluye información necesaria para la venta (ID de película, título, precio unitario, cantidad). No es la película del catálogo, sino su proyección en el proceso de compra.

- **Venta** (conceptual, fuera de alcance actual)  
  Representa una transacción confirmada. Incluye información del cliente, fecha, total y películas compradas.

### Responsabilidades de alto nivel

- **Carrito**: garantizar integridad del carrito (no duplicados, cantidades válidas), calcular totales, exponer información del carrito de forma segura.
- **PeliculaEnCarrito**: validar que los datos de la película en carrito sean consistentes (precio positivo, cantidad válida), calcular subtotales, comparar películas sin exponer detalles internos.

## Reglas de negocio relevantes

- Una película no puede agregarse dos veces al carrito; si ya existe, se incrementa la cantidad.
- El precio unitario de una película en el carrito debe ser mayor a cero.
- La cantidad de películas en el carrito debe ser al menos 1.
- No se permite eliminar una película que no está en el carrito (operación inválida).
- El total del carrito se calcula sumando los subtotales de todas las películas.
- El subtotal de cada película es: precio unitario × cantidad.

## Fuera de alcance

Esta vertical **NO** se encarga de:

- **Procesamiento de pagos externos**: integración con pasarelas de pago, validación de tarjetas, autorización bancaria.
- **Gestión de envíos**: cálculo de costos de envío, asignación de transportistas, seguimiento de pedidos.
- **Gestión del catálogo de películas**: creación, actualización o eliminación de películas en el sistema.
- **Autenticación y autorización de usuarios**: validación de credenciales, gestión de sesiones.
- **Inventario físico**: control de stock, reservas, disponibilidad en tiempo real. -> ESTE A REVISAR
- **Facturación**: emisión de facturas, comprobantes fiscales, reportes contables.

---

**Tecnologías:**  
Java 21, Spring Boot 4.0.1, Maven, H2 Database (desarrollo/docker) | React (frontend)

**Modelo de dominio:**  
Basado en programación orientada a objetos, sin frameworks en el núcleo del dominio. Las reglas de negocio residen en las entidades y agregados.

---

## Dockerización y Despliegue

### Ejecutar la vertical de forma aislada

Para ejecutar únicamente el servicio de Ventas en un contenedor:

```bash
# Construir imagen Docker
docker build -t ventas-service .

# Ejecutar contenedor
docker run -p 8083:8083 -e SPRING_PROFILES_ACTIVE=docker ventas-service
```

O usando Docker Compose:

```bash
docker-compose up --build
```

### Configuración de red para integración

El servicio de Ventas se ejecuta en el puerto **8083** y se comunica con otras verticales a través de la red Docker `almacen-net`.

**Variables de entorno requeridas:**

| Variable                 | Descripción          | Valor por defecto |
| ------------------------ | -------------------- | ----------------- |
| `SPRING_PROFILES_ACTIVE` | Perfil Spring a usar | `docker`          |
| `SERVER_PORT`            | Puerto del servicio  | `8083`            |

**Puertos expuestos:**

- `8083`: API REST de Ventas (carrito de compras)

### Integración con otras verticales

Para que la vertical de Ventas se comunique correctamente con el resto del ecosistema:

**1. Red compartida**

Todas las verticales deben estar en la misma red Docker (`almacen-net`):

```yaml
networks:
  almacen-net:
    external: true
```

**2. Nombres de servicio Docker**

Los siguientes nombres de host deben usarse para comunicación interna:

- `catalogo-backend:8080` - Servicio de catálogo de películas
- `rating-service:8082` - Servicio de ratings y valoraciones
- `ventas-service:8083` - Este servicio (Ventas)
- `api-gateway:9500` - Gateway principal (punto de entrada unificado)

**3. Configuración en API Gateway**

Para exponer este servicio a través del gateway, agregar en `application-docker.yml` del gateway:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: ventas
          uri: http://ventas-service:8083
          predicates:
            - Path=/api/ventas/**,/clientes/**
          filters:
            - StripPrefix=1
```

### Endpoints disponibles

**Base URL (interno):** `http://ventas-service:8083`  
**Base URL (externo):** `http://localhost:8083`

| Método   | Endpoint                                           | Descripción                   |
| -------- | -------------------------------------------------- | ----------------------------- |
| `GET`    | `/clientes/{clienteId}/carrito`                    | Ver carrito del cliente       |
| `POST`   | `/clientes/{clienteId}/carrito/items`              | Agregar película al carrito   |
| `DELETE` | `/clientes/{clienteId}/carrito/items/{peliculaId}` | Eliminar película del carrito |

**Ejemplo de uso con curl:**

```bash
# Ver carrito del cliente
curl http://localhost:8083/clientes/cliente-001/carrito

# Agregar película al carrito
curl -X POST http://localhost:8083/clientes/cliente-001/carrito/items \
  -H "Content-Type: application/json" \
  -d '{
    "peliculaId": "pelicula-123",
    "titulo": "Inception",
    "precioUnitario": 15.99,
    "cantidad": 2
  }'

# Eliminar película del carrito
curl -X DELETE http://localhost:8083/clientes/cliente-001/carrito/items/pelicula-123
```

### Healthcheck y monitoreo

Si Spring Boot Actuator está habilitado, el healthcheck está disponible en:

```bash
curl http://localhost:8083/actuator/health
```

### Troubleshooting

**Problema:** El servicio no arranca.  
**Solución:** Verificar logs con `docker logs ventas-service`

**Problema:** No puede comunicarse con otras verticales.  
**Solución:** Asegurar que todos los servicios estén en la misma red Docker y usar nombres de servicio (no `localhost`)

**Problema:** Error de base de datos.  
**Solución:** La vertical usa H2 en memoria (stateless). Los datos se pierden al reiniciar el contenedor.

---
