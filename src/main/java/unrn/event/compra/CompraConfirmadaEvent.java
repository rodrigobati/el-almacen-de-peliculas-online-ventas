package unrn.event.compra;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CompraConfirmadaEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        Data data) {

    public static final String EVENT_TYPE = "CompraConfirmada";
    public static final int EVENT_VERSION = 1;
    public static final String PRODUCER = "ventas-service";

    public CompraConfirmadaEvent(Data data) {
        this(UUID.randomUUID(), EVENT_TYPE, EVENT_VERSION, Instant.now(), PRODUCER, data);
    }

    public record Data(
            UUID compraId,
            String clienteEmail,
            Instant fechaConfirmacion,
            List<ItemCompraConfirmada> items,
            TotalCompraConfirmada total) {

        public Data {
            items = List.copyOf(items);
        }
    }

    public record ItemCompraConfirmada(String titulo, int cantidad, BigDecimal precioUnitario) {
    }

    public record TotalCompraConfirmada(
            BigDecimal totalBruto,
            BigDecimal descuento,
            String descuentoDescripcion) {
    }
}
