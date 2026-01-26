package unrn.dto;

import java.math.BigDecimal;
import java.util.List;

public record CarritoDTO(
        List<PeliculaEnCarritoDTO> items,
        BigDecimal total) {
}
