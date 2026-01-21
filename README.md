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
Java 23, Maven, MySQL (backend) | React (frontend)

**Modelo de dominio:**  
Basado en programación orientada a objetos, sin frameworks en el núcleo del dominio. Las reglas de negocio residen en las entidades y agregados.
