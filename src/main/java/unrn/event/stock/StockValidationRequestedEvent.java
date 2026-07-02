package unrn.event.stock;

import java.time.Instant;
import java.util.List;

public record StockValidationRequestedEvent(
        String eventId,
        Long compraId,
        List<Item> items,
        Instant occurredAt) {

    public static final String EVENT_TYPE = "StockValidationRequested";

    public StockValidationRequestedEvent {
        items = List.copyOf(items);
    }

    public record Item(
            Long peliculaId,
            int cantidad) {
    }
}