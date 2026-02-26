package unrn.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record ValidarCuponResponse(
        boolean valido,
        BigDecimal porcentajeDescuento,
        Instant vigenteDesde,
        Instant vigenteHasta,
        String motivo
) implements Serializable {}