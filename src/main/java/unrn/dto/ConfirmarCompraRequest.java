package unrn.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ConfirmarCompraRequest(
        String nombreCupon,
        BigDecimal porcentajeDescuento,
        Instant vigenteDesde,
        Instant vigenteHasta) {
}