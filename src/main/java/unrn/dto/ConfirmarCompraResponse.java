package unrn.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ConfirmarCompraResponse(
                Long compraId,
                Instant fechaHora,
                BigDecimal totalFinal,
                String estado) {
}