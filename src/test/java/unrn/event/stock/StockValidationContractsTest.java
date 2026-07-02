package unrn.event.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockValidationContractsTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("StockValidationRequestedEvent serializacionMantieneContratoEsperado")
    void stockValidationRequestedEvent_serializacionMantieneContratoEsperado() throws Exception {
        // Setup: evento request con un item minimo
        StockValidationRequestedEvent event = new StockValidationRequestedEvent(
                "evt-req-1",
                55L,
                List.of(new StockValidationRequestedEvent.Item(10L, 2)),
                Instant.parse("2026-06-09T10:15:30Z"));

        // Ejercitación: serializar a JSON
        String json = objectMapper.writeValueAsString(event);
        JsonNode root = objectMapper.readTree(json);
        JsonNode item = root.get("items").get(0);

        // Verificación: contrato del evento y forma minima de item
        assertTrue(root.has("eventId"), "El contrato debe incluir eventId");
        assertTrue(root.has("compraId"), "El contrato debe incluir compraId");
        assertTrue(root.has("items"), "El contrato debe incluir items");
        assertTrue(root.has("occurredAt"), "El contrato debe incluir occurredAt");
        assertEquals("evt-req-1", root.get("eventId").asText(), "eventId debe preservarse");
        assertEquals(55L, root.get("compraId").asLong(), "compraId debe preservarse");

        assertEquals(2, item.size(), "Cada item debe incluir solo peliculaId y cantidad");
        assertTrue(item.has("peliculaId"), "Cada item debe incluir peliculaId");
        assertTrue(item.has("cantidad"), "Cada item debe incluir cantidad");
        assertFalse(item.has("titulo"), "El item no debe incluir titulo");
        assertFalse(item.has("precio"), "El item no debe incluir precio");
    }

    @Test
    @DisplayName("StockValidationAcceptedEvent deserializacionDesdeJsonCompatiblePreservaCampos")
    void stockValidationAcceptedEvent_deserializacionDesdeJsonCompatiblePreservaCampos() throws Exception {
        // Setup: JSON compatible publicado por catalogo
        String json = """
                {
                  "eventId": "evt-acc-1",
                  "compraId": 77,
                  "occurredAt": "2026-06-09T11:00:00Z"
                }
                """;

        // Ejercitación: deserializar en ventas
        StockValidationAcceptedEvent event = objectMapper.readValue(json, StockValidationAcceptedEvent.class);

        // Verificación: preserva correlacion y compra
        assertEquals("evt-acc-1", event.eventId(), "eventId debe preservarse");
        assertEquals(77L, event.compraId(), "compraId debe preservarse");
        assertEquals(Instant.parse("2026-06-09T11:00:00Z"), event.occurredAt(), "occurredAt debe preservarse");
    }

    @Test
    @DisplayName("StockRechazadoEvent deserializacionDesdeJsonCompatiblePreservaCampos")
    void stockRechazadoEvent_deserializacionDesdeJsonCompatiblePreservaCampos() throws Exception {
        // Setup: JSON compatible de rechazo publicado por catalogo
        String json = """
                {
                  "eventId": "evt-rej-1",
                  "compraId": 88,
                  "motivo": "STOCK_INSUFICIENTE",
                  "detalles": [
                    {
                      "peliculaId": 10,
                      "solicitado": 3,
                      "disponible": "1"
                    }
                  ]
                }
                """;

        // Ejercitación: deserializar en ventas
        StockRechazadoEvent event = objectMapper.readValue(json, StockRechazadoEvent.class);

        // Verificación: contrato compatible en rechazo
        assertEquals("evt-rej-1", event.eventId(), "eventId debe preservarse");
        assertEquals(88L, event.compraId(), "compraId debe preservarse");
        assertEquals("STOCK_INSUFICIENTE", event.motivo(), "motivo debe preservarse");
        assertEquals(1, event.detalles().size(), "detalles debe conservar elementos");
        assertEquals(10L, event.detalles().get(0).peliculaId(), "peliculaId del detalle debe preservarse");
        assertEquals(3, event.detalles().get(0).solicitado(), "solicitado del detalle debe preservarse");
        assertEquals("1", event.detalles().get(0).disponible(), "disponible del detalle debe preservarse");
    }
}
