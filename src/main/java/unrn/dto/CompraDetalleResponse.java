package unrn.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CompraDetalleResponse(
                Long compraId,
                Instant fechaHora,
                BigDecimal subtotal,
                BigDecimal descuentoAplicado,
                BigDecimal totalFinal,
                String estado,
                String motivoRechazo,
                String detallesRechazo,
                List<CompraItemResponse> items) {
}