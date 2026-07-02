package unrn.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RabbitMqStockValidationRoutingContractTest {

    @Test
    @DisplayName("RoutingStockValidation propiedadesVentasMantienenContratoConCatalogo")
    void routingStockValidation_propiedadesVentasMantienenContratoConCatalogo() throws Exception {
        // Setup: leer propiedades productivas del modulo ventas
        Properties properties = new Properties();
                Path mainPropertiesPath = Path.of("src", "main", "resources", "application.properties");
                try (InputStream input = Files.newInputStream(mainPropertiesPath)) {
            assertNotNull(input, "Debe existir application.properties en classpath");
            properties.load(input);
        }

        // Ejercitación y Verificación: request flow ventas -> catalogo
        assertEquals("ventas.events", properties.getProperty("rabbitmq.ventas.events.exchange"),
                "El exchange de request debe coincidir con el contrato ventas -> catalogo");
        assertEquals("catalogo.stock.validation.requested",
                properties.getProperty("rabbitmq.catalogo.stock.validation.requested.routing-key"),
                "El routing key de request debe coincidir con el contrato");

        // Verificación: result flow catalogo -> ventas
        assertEquals("catalogo.events", properties.getProperty("rabbitmq.catalogo.events.exchange"),
                "El exchange de resultados debe coincidir con el contrato");
        assertEquals("catalogo.stock.validation.accepted",
                properties.getProperty("rabbitmq.catalogo.stock.validation.accepted.routing-key"),
                "El routing key accepted debe coincidir con el contrato");
        assertEquals("catalogo.stock.rechazado",
                properties.getProperty("rabbitmq.catalogo.stock.rechazado.routing-key"),
                "El routing key rechazado debe coincidir con el contrato");
        assertEquals("ventas.q.catalogo-stock-validation-accepted",
                properties.getProperty("rabbitmq.ventas.stock.validation.accepted.queue"),
                "La cola de accepted en ventas debe coincidir con el contrato");
        assertEquals("ventas.q.catalogo-stock-rechazado",
                properties.getProperty("rabbitmq.ventas.stock.rechazado.queue"),
                "La cola de rechazado en ventas debe coincidir con el contrato");
    }
}
