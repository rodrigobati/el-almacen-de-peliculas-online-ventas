package unrn.event.stock;

import java.util.List;

public record StockRechazadoEvent(
        String eventId,
        Long compraId,
        String motivo,
        List<DetalleStockRechazado> detalles) {

    public record DetalleStockRechazado(
            Long peliculaId,
            int solicitado,
            String disponible) {
    }
}
