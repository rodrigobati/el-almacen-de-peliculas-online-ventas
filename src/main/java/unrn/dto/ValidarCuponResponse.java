package unrn.dto;

import java.io.Serializable;
import java.time.LocalDate;

public record ValidarCuponResponse(
        boolean valido,
        Float porcentajeDescuento,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        String motivo
) implements Serializable {}