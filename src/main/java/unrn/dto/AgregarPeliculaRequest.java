package unrn.dto;

import java.math.BigDecimal;

public record AgregarPeliculaRequest(
        String peliculaId,
        String titulo,
        BigDecimal precioUnitario,
        int cantidad) {
}
