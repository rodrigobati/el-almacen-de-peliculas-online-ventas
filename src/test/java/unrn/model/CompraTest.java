package unrn.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CompraTest {

    @Test
    @DisplayName("comprar guardaSnapshotInmutable")
    void comprar_guardaSnapshotInmutable() {
        // Setup: Preparar el escenario
        var proyeccion = new PeliculaProyeccion("1", "Titulo", new BigDecimal("1000.0"), true, 1L);
        var compra = new Compra();

        // Ejercitación: Ejecutar la acción a probar
        compra.agregarDetalleDesde(proyeccion, 2);

        // Verificación: Verificar el resultado esperado
        var detalle = compra.detalles().get(0);
        assertEquals("Titulo", detalle.tituloAlComprar(), "El título del snapshot debe persistir");
        assertEquals(new BigDecimal("1000.0"), detalle.precioAlComprar(), "El precio del snapshot debe persistir");
    }
}
