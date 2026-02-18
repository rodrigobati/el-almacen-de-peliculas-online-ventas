package unrn.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CarritoTest {

    @Test
    @DisplayName("AgregarPelicula con id nulo lanza excepción")
    void agregarPelicula_idNulo_lanzaExcepcion() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();
        String peliculaId = null;

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            carrito.agregarPelicula(peliculaId, "Matrix", new BigDecimal("100.00"), 1);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(Carrito.ERROR_PELICULA_ID_NULO, ex.getMessage());
    }

    @Test
    @DisplayName("AgregarPelicula con id vacío lanza excepción")
    void agregarPelicula_idVacio_lanzaExcepcion() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();
        String peliculaId = "   ";

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            carrito.agregarPelicula(peliculaId, "Matrix", new BigDecimal("100.00"), 1);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(Carrito.ERROR_PELICULA_ID_VACIO, ex.getMessage());
    }

    @Test
    @DisplayName("AgregarPelicula con cantidad cero o negativa lanza excepción")
    void agregarPelicula_cantidadCeroONegativa_lanzaExcepcion() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            carrito.agregarPelicula("1", "Matrix", new BigDecimal("100.00"), 0);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(Carrito.ERROR_CANTIDAD_INVALIDA, ex.getMessage());
    }

    @Test
    @DisplayName("Constructor con lista conteniendo null lanza excepción")
    void constructor_listaConNull_lanzaExcepcion() {
        // Setup: Preparar el escenario
        List<PeliculaEnCarrito> items = new ArrayList<>();
        items.add(new PeliculaEnCarrito("1", "Matrix", new BigDecimal("100.00"), 1));
        items.add(null);

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            new Carrito(items);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(Carrito.ERROR_ITEM_NULO, ex.getMessage());
    }

    @Test
    @DisplayName("AgregarPelicula con datos válidos agrega item y total correcto")
    void agregarPelicula_conDatosValidos_agregaItemYTotalCorrecto() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();
        String peliculaId = "1";
        String titulo = "Matrix";
        BigDecimal precio = new BigDecimal("100.00");
        int cantidad = 2;

        // Ejercitación: Ejecutar la acción a probar
        carrito.agregarPelicula(peliculaId, titulo, precio, cantidad);

        // Verificación: Verificar el resultado esperado
        assertEquals(1, carrito.items().size(), "El carrito debe tener 1 item");
        BigDecimal totalEsperado = new BigDecimal("200.00");
        assertEquals(totalEsperado, carrito.total(), "El total debe ser 200.00");
    }

    @Test
    @DisplayName("AgregarPelicula agregando misma película incrementa cantidad no duplica")
    void agregarPelicula_agregandoMismaPelicula_incrementaCantidadNoDuplica() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();
        String peliculaId = "1";
        String titulo = "Matrix";
        BigDecimal precio = new BigDecimal("100.00");

        // Ejercitación: Ejecutar la acción a probar
        carrito.agregarPelicula(peliculaId, titulo, precio, 2);
        carrito.agregarPelicula(peliculaId, titulo, precio, 3);

        // Verificación: Verificar el resultado esperado
        assertEquals(1, carrito.items().size(), "El carrito debe seguir teniendo 1 item");
        BigDecimal totalEsperado = new BigDecimal("500.00");
        assertEquals(totalEsperado, carrito.total(), "El total debe ser 500.00 (5 * 100)");
    }

    @Test
    @DisplayName("EliminarPelicula con id nulo lanza excepción")
    void eliminarPelicula_conIdNulo_lanzaExcepcion() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();
        String peliculaId = null;

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            carrito.eliminarPelicula(peliculaId);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(Carrito.ERROR_PELICULA_ID_NULO, ex.getMessage());
    }

    @Test
    @DisplayName("EliminarPelicula con id vacío lanza excepción")
    void eliminarPelicula_conIdVacio_lanzaExcepcion() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();
        String peliculaId = "   ";

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            carrito.eliminarPelicula(peliculaId);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(Carrito.ERROR_PELICULA_ID_VACIO, ex.getMessage());
    }

    @Test
    @DisplayName("EliminarPelicula con id inexistente lanza excepción")
    void eliminarPelicula_conIdInexistente_lanzaExcepcion() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();
        carrito.agregarPelicula("1", "Matrix", new BigDecimal("100.00"), 1);
        String peliculaIdInexistente = "999";

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            carrito.eliminarPelicula(peliculaIdInexistente);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(Carrito.ERROR_ITEM_NO_ENCONTRADO, ex.getMessage());
    }

    @Test
    @DisplayName("EliminarPelicula con id existente elimina y total actualiza")
    void eliminarPelicula_conIdExistente_eliminaYTotalActualiza() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();
        carrito.agregarPelicula("1", "Matrix", new BigDecimal("100.00"), 2);
        carrito.agregarPelicula("2", "Avatar", new BigDecimal("150.00"), 1);
        BigDecimal totalInicial = carrito.total();
        assertEquals(new BigDecimal("350.00"), totalInicial, "El total inicial debe ser 350.00");

        // Ejercitación: Ejecutar la acción a probar
        carrito.eliminarPelicula("1");

        // Verificación: Verificar el resultado esperado
        assertEquals(1, carrito.items().size(), "El carrito debe tener 1 item");
        BigDecimal totalFinal = carrito.total();
        assertEquals(new BigDecimal("150.00"), totalFinal, "El total final debe ser 150.00");
    }

    @Test
    @DisplayName("DecrementarPelicula con cantidad mayor a uno reduce cantidad")
    void decrementarPelicula_cantidadMayorAUno_reduceCantidad() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();
        carrito.agregarPelicula("1", "Matrix", new BigDecimal("100.00"), 3);

        // Ejercitación: Ejecutar la acción a probar
        carrito.decrementarPelicula("1");

        // Verificación: Verificar el resultado esperado
        assertEquals(1, carrito.items().size(), "El carrito debe seguir teniendo 1 item");
        assertEquals(2, carrito.items().get(0).cantidad(), "La cantidad debe ser 2");
    }

    @Test
    @DisplayName("DecrementarPelicula con cantidad uno elimina el item")
    void decrementarPelicula_cantidadUno_eliminaItem() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();
        carrito.agregarPelicula("1", "Matrix", new BigDecimal("100.00"), 1);

        // Ejercitación: Ejecutar la acción a probar
        carrito.decrementarPelicula("1");

        // Verificación: Verificar el resultado esperado
        assertTrue(carrito.items().isEmpty(), "El carrito debe quedar vacío");
        assertEquals(BigDecimal.ZERO, carrito.total(), "El total debe ser cero");
    }

    @Test
    @DisplayName("Constructor con lista nula lanza excepción")
    void constructor_conListaNula_lanzaExcepcion() {
        // Setup: Preparar el escenario
        List<PeliculaEnCarrito> items = null;

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            new Carrito(items);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(Carrito.ERROR_ITEMS_NULOS, ex.getMessage());
    }

    @Test
    @DisplayName("Constructor con duplicados por id lanza excepción")
    void constructor_conDuplicadosPorId_lanzaExcepcion() {
        // Setup: Preparar el escenario
        List<PeliculaEnCarrito> items = new ArrayList<>();
        items.add(new PeliculaEnCarrito("1", "Matrix", new BigDecimal("100.00"), 1));
        items.add(new PeliculaEnCarrito("1", "Matrix Reloaded", new BigDecimal("120.00"), 2));

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            new Carrito(items);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(Carrito.ERROR_PELICULAS_DUPLICADAS, ex.getMessage());
    }

    @Test
    @DisplayName("ConfirmarCompra con cliente nulo lanza excepción")
    void confirmarCompra_clienteNulo_lanzaExcepcion() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();
        carrito.agregarPelicula("1", "Matrix", new BigDecimal("100.00"), 1);

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            carrito.confirmarCompra(null, Instant.parse("2026-02-17T10:15:30Z"));
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(Carrito.ERROR_CLIENTE_NULO, ex.getMessage());
    }

    @Test
    @DisplayName("ConfirmarCompra con carrito vacío lanza excepción")
    void confirmarCompra_carritoVacio_lanzaExcepcion() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();
        Cliente cliente = new Cliente("cliente-1");

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            carrito.confirmarCompra(cliente, Instant.parse("2026-02-17T10:15:30Z"));
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(Carrito.ERROR_CARRITO_VACIO, ex.getMessage());
    }

    @Test
    @DisplayName("ConfirmarCompra con descuento vigente aplica descuento y vacía carrito")
    void confirmarCompra_descuentoVigente_aplicaDescuentoYVaciaCarrito() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();
        carrito.agregarPelicula("1", "Matrix", new BigDecimal("100.00"), 2);
        Cliente cliente = new Cliente("cliente-1");
        Instant fechaCompra = Instant.parse("2026-02-17T10:15:30Z");
        Descuento descuento = new Descuento(
                new BigDecimal("10"),
                Instant.parse("2026-02-01T00:00:00Z"),
                Instant.parse("2026-02-28T23:59:59Z"));

        // Ejercitación: Ejecutar la acción a probar
        Compra compra = carrito.confirmarCompra(cliente, fechaCompra, descuento);

        // Verificación: Verificar el resultado esperado
        assertTrue(compra.cliente().esElMismoQue(cliente), "La compra debe pertenecer al cliente autenticado");
        assertEquals(fechaCompra, compra.fechaHoraCompra(), "La compra debe guardar el instante recibido");
        assertEquals(new BigDecimal("200.00"), compra.subtotal(), "El subtotal debe ser 200.00");
        assertEquals(new BigDecimal("20.00"), compra.descuentoAplicado(), "El descuento aplicado debe ser 20.00");
        assertEquals(new BigDecimal("180.00"), compra.total(), "El total final debe ser 180.00");
        assertTrue(carrito.items().isEmpty(), "El carrito debe quedar vacío luego de confirmar compra");
    }

    @Test
    @DisplayName("ConfirmarCompra con descuento fuera de vigencia no aplica descuento")
    void confirmarCompra_descuentoFueraDeVigencia_noAplicaDescuento() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();
        carrito.agregarPelicula("1", "Matrix", new BigDecimal("100.00"), 2);
        Cliente cliente = new Cliente("cliente-1");
        Instant fechaCompra = Instant.parse("2026-02-17T10:15:30Z");
        Descuento descuento = new Descuento(
                new BigDecimal("10"),
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-03-31T23:59:59Z"));

        // Ejercitación: Ejecutar la acción a probar
        Compra compra = carrito.confirmarCompra(cliente, fechaCompra, descuento);

        // Verificación: Verificar el resultado esperado
        assertEquals(new BigDecimal("0"), compra.descuentoAplicado(), "No debe aplicarse descuento fuera de vigencia");
        assertEquals(new BigDecimal("200.00"), compra.total(), "El total debe ser igual al subtotal sin descuento");
    }

    @Test
    @DisplayName("ConfirmarCompra guarda snapshot de precio en detalle")
    void confirmarCompra_guardaSnapshotPrecioEnDetalle() {
        // Setup: Preparar el escenario
        Carrito carrito = new Carrito();
        carrito.agregarPelicula("1", "Matrix", new BigDecimal("120.00"), 1);
        Cliente cliente = new Cliente("cliente-1");

        // Ejercitación: Ejecutar la acción a probar
        Compra compra = carrito.confirmarCompra(cliente, Instant.parse("2026-02-17T10:15:30Z"));

        // Verificación: Verificar el resultado esperado
        DetalleCompra detalle = compra.detalles().get(0);
        assertEquals(new BigDecimal("120.00"), detalle.precioAlComprar(),
                "El detalle debe conservar el precio al confirmar compra");
    }
}
