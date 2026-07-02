package unrn.event.stock;

import java.time.Instant;

public record StockValidationAcceptedEvent(
        String eventId,
        Long compraId,
        Instant occurredAt) {
}
