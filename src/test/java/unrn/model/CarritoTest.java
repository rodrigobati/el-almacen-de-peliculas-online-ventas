package unrn.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
}
