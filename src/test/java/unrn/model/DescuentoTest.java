package unrn.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DescuentoTest {

    @Test
    @DisplayName("Constructor con rango de fechas inválido lanza excepción")
    void constructor_rangoFechasInvalido_lanzaExcepcion() {
        // Setup: Preparar el escenario
        BigDecimal porcentaje = new BigDecimal("10");
        Instant desde = Instant.parse("2026-02-28T00:00:00Z");
        Instant hasta = Instant.parse("2026-02-01T00:00:00Z");

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            new Descuento(porcentaje, desde, hasta);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(Descuento.ERROR_RANGO_FECHAS_INVALIDO, ex.getMessage());
    }

    @Test
    @DisplayName("MontoAplicadoSobre con descuento vigente calcula monto esperado")
    void montoAplicadoSobre_descuentoVigente_calculaMontoEsperado() {
        // Setup: Preparar el escenario
        Descuento descuento = new Descuento(
                new BigDecimal("15"),
                Instant.parse("2026-02-01T00:00:00Z"),
                Instant.parse("2026-02-28T23:59:59Z"));

        // Ejercitación: Ejecutar la acción a probar
        BigDecimal monto = descuento.montoAplicadoSobre(
                new BigDecimal("200.00"),
                Instant.parse("2026-02-17T10:15:30Z"));

        // Verificación: Verificar el resultado esperado
        assertEquals(new BigDecimal("30.00"), monto, "El descuento vigente del 15% sobre 200 debe ser 30.00");
    }

    @Test
    @DisplayName("MontoAplicadoSobre fuera de vigencia devuelve cero")
    void montoAplicadoSobre_fueraDeVigencia_devuelveCero() {
        // Setup: Preparar el escenario
        Descuento descuento = new Descuento(
                new BigDecimal("15"),
                Instant.parse("2026-02-01T00:00:00Z"),
                Instant.parse("2026-02-28T23:59:59Z"));

        // Ejercitación: Ejecutar la acción a probar
        BigDecimal monto = descuento.montoAplicadoSobre(
                new BigDecimal("200.00"),
                Instant.parse("2026-03-17T10:15:30Z"));

        // Verificación: Verificar el resultado esperado
        assertEquals(BigDecimal.ZERO, monto, "Fuera de vigencia no debe aplicarse descuento");
    }
}