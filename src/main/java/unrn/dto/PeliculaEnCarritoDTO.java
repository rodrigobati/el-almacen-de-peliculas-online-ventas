package unrn.dto;

import java.math.BigDecimal;

public record PeliculaEnCarritoDTO(
        String peliculaId,
        String titulo,
        BigDecimal precioUnitario,
        int cantidad,
        BigDecimal subtotal) {
}
