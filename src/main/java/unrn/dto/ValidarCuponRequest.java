package unrn.dto;

import java.io.Serializable;

public record ValidarCuponRequest(
        String nombreCupon
) implements Serializable {}