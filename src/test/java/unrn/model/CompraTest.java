package unrn.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompraTest {

    @Test
    @DisplayName("Constructor calcula total y mantiene detalles de solo lectura")
    void constructor_calculaTotalYMantieneDetallesDeSoloLectura() {
        // Setup: Preparar el escenario
        Cliente cliente = new Cliente("cliente-1");
        Instant fecha = Instant.parse("2026-02-17T10:15:30Z");
        List<DetalleCompra> detalles = new ArrayList<>();
        detalles.add(new DetalleCompra("1", "Matrix", new BigDecimal("100.00"), 2));
        Compra compra = new Compra(cliente, fecha, detalles, new BigDecimal("200.00"), new BigDecimal("20.00"));

        // Ejercitación: Ejecutar la acción a probar
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            compra.detalles().add(new DetalleCompra("2", "Avatar", new BigDecimal("80.00"), 1));
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(UnsupportedOperationException.class, ex.getClass(), "Los detalles deben ser inmutables");
        assertEquals(new BigDecimal("180.00"), compra.total(), "El total debe ser subtotal menos descuento");
    }

    @Test
    @DisplayName("Constructor con descuento mayor al subtotal lanza excepción")
    void constructor_descuentoMayorASubtotal_lanzaExcepcion() {
        // Setup: Preparar el escenario
        Cliente cliente = new Cliente("cliente-1");
        Instant fecha = Instant.parse("2026-02-17T10:15:30Z");
        List<DetalleCompra> detalles = List.of(new DetalleCompra("1", "Matrix", new BigDecimal("100.00"), 1));

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            new Compra(cliente, fecha, detalles, new BigDecimal("100.00"), new BigDecimal("150.00"));
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(Compra.ERROR_DESCUENTO_MAYOR_A_SUBTOTAL, ex.getMessage());
    }
}
