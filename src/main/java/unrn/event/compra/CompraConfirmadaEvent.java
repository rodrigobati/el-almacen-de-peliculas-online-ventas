package unrn.event.compra;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CompraConfirmadaEvent(
        String eventId,
        Long compraId,
        String clienteId,
        Instant fechaHora,
        List<ItemCompraConfirmada> items) {

    public CompraConfirmadaEvent(Long compraId, String clienteId, Instant fechaHora, List<ItemCompraConfirmada> items) {
        this(UUID.randomUUID().toString(), compraId, clienteId, fechaHora, List.copyOf(items));
    }

    public record ItemCompraConfirmada(Long peliculaId, int cantidad) {
    }
}
