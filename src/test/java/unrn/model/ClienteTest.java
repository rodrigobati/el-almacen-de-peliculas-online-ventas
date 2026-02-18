package unrn.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClienteTest {

    @Test
    @DisplayName("Constructor con clienteId nulo lanza excepción")
    void constructor_clienteIdNulo_lanzaExcepcion() {
        // Setup: Preparar el escenario
        String clienteId = null;

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, () -> {
            new Cliente(clienteId);
        });

        // Verificación: Verificar el resultado esperado
        assertEquals(Cliente.ERROR_CLIENTE_ID_NULO, ex.getMessage());
    }

    @Test
    @DisplayName("EsElMismoQue con mismo clienteId devuelve true")
    void esElMismoQue_mismoClienteId_devuelveTrue() {
        // Setup: Preparar el escenario
        Cliente cliente = new Cliente("cliente-1");
        Cliente otro = new Cliente("cliente-1");

        // Ejercitación: Ejecutar la acción a probar
        boolean resultado = cliente.esElMismoQue(otro);

        // Verificación: Verificar el resultado esperado
        assertTrue(resultado, "Dos clientes con el mismo id deben considerarse el mismo cliente");
    }
}