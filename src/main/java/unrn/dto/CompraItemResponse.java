package unrn.dto;

import java.math.BigDecimal;

public record CompraItemResponse(
        String peliculaId,
        String titulo,
        BigDecimal precioAlComprar,
        int cantidad,
        BigDecimal subtotal) {
}