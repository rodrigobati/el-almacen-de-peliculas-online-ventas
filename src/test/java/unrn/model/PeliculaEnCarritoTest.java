package unrn.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PeliculaEnCarritoTest {

    @Test
    @DisplayName("Constructor con peliculaId nulo lanza excepción")
    void constructor_conPeliculaIdNulo_lanzaExcepcion() {
        // Setup: Preparar el escenario
        String peliculaId = null;
        String titulo = "Matrix";
        BigDecimal precio = new BigDecimal("100.00");
        int cantidad = 1;

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            new PeliculaEnCarrito(peliculaId, titulo, precio, cantidad);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(PeliculaEnCarrito.ERROR_PELICULA_ID_NULO, ex.getMessage());
    }

    @Test
    @DisplayName("Constructor con peliculaId vacío lanza excepción")
    void constructor_conPeliculaIdVacio_lanzaExcepcion() {
        // Setup: Preparar el escenario
        String peliculaId = "   ";
        String titulo = "Matrix";
        BigDecimal precio = new BigDecimal("100.00");
        int cantidad = 1;

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            new PeliculaEnCarrito(peliculaId, titulo, precio, cantidad);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(PeliculaEnCarrito.ERROR_PELICULA_ID_VACIO, ex.getMessage());
    }

    @Test
    @DisplayName("Constructor con titulo nulo lanza excepción")
    void constructor_conTituloNulo_lanzaExcepcion() {
        // Setup: Preparar el escenario
        String peliculaId = "1";
        String titulo = null;
        BigDecimal precio = new BigDecimal("100.00");
        int cantidad = 1;

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            new PeliculaEnCarrito(peliculaId, titulo, precio, cantidad);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(PeliculaEnCarrito.ERROR_TITULO_NULO, ex.getMessage());
    }

    @Test
    @DisplayName("Constructor con titulo vacío lanza excepción")
    void constructor_conTituloVacio_lanzaExcepcion() {
        // Setup: Preparar el escenario
        String peliculaId = "1";
        String titulo = "   ";
        BigDecimal precio = new BigDecimal("100.00");
        int cantidad = 1;

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            new PeliculaEnCarrito(peliculaId, titulo, precio, cantidad);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(PeliculaEnCarrito.ERROR_TITULO_VACIO, ex.getMessage());
    }

    @Test
    @DisplayName("Constructor con precio nulo lanza excepción")
    void constructor_conPrecioNulo_lanzaExcepcion() {
        // Setup: Preparar el escenario
        String peliculaId = "1";
        String titulo = "Matrix";
        BigDecimal precio = null;
        int cantidad = 1;

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            new PeliculaEnCarrito(peliculaId, titulo, precio, cantidad);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(PeliculaEnCarrito.ERROR_PRECIO_NULO, ex.getMessage());
    }

    @Test
    @DisplayName("Constructor con precio cero o negativo lanza excepción")
    void constructor_conPrecioCeroONegativo_lanzaExcepcion() {
        // Setup: Preparar el escenario
        String peliculaId = "1";
        String titulo = "Matrix";
        BigDecimal precioCero = BigDecimal.ZERO;
        int cantidad = 1;

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            new PeliculaEnCarrito(peliculaId, titulo, precioCero, cantidad);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(PeliculaEnCarrito.ERROR_PRECIO_INVALIDO, ex.getMessage());
    }

    @Test
    @DisplayName("Constructor con cantidad cero o negativa lanza excepción")
    void constructor_conCantidadCeroONegativa_lanzaExcepcion() {
        // Setup: Preparar el escenario
        String peliculaId = "1";
        String titulo = "Matrix";
        BigDecimal precio = new BigDecimal("100.00");
        int cantidad = 0;

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            new PeliculaEnCarrito(peliculaId, titulo, precio, cantidad);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(PeliculaEnCarrito.ERROR_CANTIDAD_INVALIDA, ex.getMessage());
    }

    @Test
    @DisplayName("IncrementarCantidad con delta cero o negativo lanza excepción")
    void incrementarCantidad_conDeltaCeroONegativo_lanzaExcepcion() {
        // Setup: Preparar el escenario
        PeliculaEnCarrito pelicula = new PeliculaEnCarrito("1", "Matrix", new BigDecimal("100.00"), 1);
        int delta = 0;

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            pelicula.incrementarCantidad(delta);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(PeliculaEnCarrito.ERROR_DELTA_INVALIDO, ex.getMessage());
    }

    @Test
    @DisplayName("IncrementarCantidad con delta válido incrementa cantidad y subtotal correcto")
    void incrementarCantidad_conDeltaValido_incrementaCantidadYSubtotalCorrecto() {
        // Setup: Preparar el escenario
        BigDecimal precio = new BigDecimal("100.00");
        PeliculaEnCarrito pelicula = new PeliculaEnCarrito("1", "Matrix", precio, 2);
        BigDecimal subtotalInicial = pelicula.subtotal();
        assertEquals(new BigDecimal("200.00"), subtotalInicial, "El subtotal inicial debe ser 200.00");

        // Ejercitación: Ejecutar la acción a probar
        pelicula.incrementarCantidad(3);

        // Verificación: Verificar el resultado esperado
        BigDecimal subtotalFinal = pelicula.subtotal();
        assertEquals(new BigDecimal("500.00"), subtotalFinal, "El subtotal final debe ser 500.00 (5 * 100)");
    }

    @Test
    @DisplayName("Absorber con misma película suma cantidad")
    void absorber_mismaPelicula_sumaCantidad() {
        // Setup: Preparar el escenario
        PeliculaEnCarrito pelicula1 = new PeliculaEnCarrito("1", "Matrix", new BigDecimal("100.00"), 2);
        PeliculaEnCarrito pelicula2 = new PeliculaEnCarrito("1", "Matrix", new BigDecimal("100.00"), 3);
        BigDecimal subtotalInicial = pelicula1.subtotal();
        assertEquals(new BigDecimal("200.00"), subtotalInicial, "El subtotal inicial debe ser 200.00");

        // Ejercitación: Ejecutar la acción a probar
        pelicula1.absorber(pelicula2);

        // Verificación: Verificar el resultado esperado
        BigDecimal subtotalFinal = pelicula1.subtotal();
        assertEquals(new BigDecimal("500.00"), subtotalFinal, "El subtotal final debe ser 500.00 (5 * 100)");
    }

    @Test
    @DisplayName("Absorber con película nula lanza excepción")
    void absorber_peliculaNula_lanzaExcepcion() {
        // Setup: Preparar el escenario
        PeliculaEnCarrito pelicula = new PeliculaEnCarrito("1", "Matrix", new BigDecimal("100.00"), 1);

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            pelicula.absorber(null);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(PeliculaEnCarrito.ERROR_PELICULA_A_ABSORBER_NULA, ex.getMessage());
    }

    @Test
    @DisplayName("Absorber con película distinta lanza excepción")
    void absorber_peliculaDistinta_lanzaExcepcion() {
        // Setup: Preparar el escenario
        PeliculaEnCarrito pelicula1 = new PeliculaEnCarrito("1", "Matrix", new BigDecimal("100.00"), 1);
        PeliculaEnCarrito pelicula2 = new PeliculaEnCarrito("2", "Avatar", new BigDecimal("150.00"), 1);

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            pelicula1.absorber(pelicula2);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(PeliculaEnCarrito.ERROR_PELICULA_DISTINTA, ex.getMessage());
    }
}
