package unrn.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public class Descuento {

    static final String ERROR_PORCENTAJE_NULO = "El porcentaje de descuento no puede ser nulo";
    static final String ERROR_PORCENTAJE_INVALIDO = "El porcentaje de descuento debe estar entre 0 y 100";
    static final String ERROR_VIGENCIA_DESDE_NULA = "La fecha de vigencia desde no puede ser nula";
    static final String ERROR_VIGENCIA_HASTA_NULA = "La fecha de vigencia hasta no puede ser nula";
    static final String ERROR_RANGO_FECHAS_INVALIDO = "La fecha desde no puede ser posterior a la fecha hasta";
    static final String ERROR_TOTAL_NULO = "El total no puede ser nulo";
    static final String ERROR_TOTAL_NEGATIVO = "El total no puede ser negativo";
    static final String ERROR_FECHA_HORA_NULA = "La fecha y hora no puede ser nula";

    private final BigDecimal porcentaje;
    private final Instant vigenteDesde;
    private final Instant vigenteHasta;

    public Descuento(BigDecimal porcentaje, Instant vigenteDesde, Instant vigenteHasta) {
        assertPorcentajeNoNulo(porcentaje);
        assertPorcentajeValido(porcentaje);
        assertVigenciaDesdeNoNula(vigenteDesde);
        assertVigenciaHastaNoNula(vigenteHasta);
        assertRangoFechasValido(vigenteDesde, vigenteHasta);

        this.porcentaje = porcentaje;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
    }

    public static Descuento sinDescuento() {
        return new Descuento(BigDecimal.ZERO, Instant.EPOCH, Instant.EPOCH);
    }

    private void assertPorcentajeNoNulo(BigDecimal porcentaje) {
        if (porcentaje == null) {
            throw new RuntimeException(ERROR_PORCENTAJE_NULO);
        }
    }

    private void assertPorcentajeValido(BigDecimal porcentaje) {
        if (porcentaje.compareTo(BigDecimal.ZERO) < 0 || porcentaje.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RuntimeException(ERROR_PORCENTAJE_INVALIDO);
        }
    }

    private void assertVigenciaDesdeNoNula(Instant vigenteDesde) {
        if (vigenteDesde == null) {
            throw new RuntimeException(ERROR_VIGENCIA_DESDE_NULA);
        }
    }

    private void assertVigenciaHastaNoNula(Instant vigenteHasta) {
        if (vigenteHasta == null) {
            throw new RuntimeException(ERROR_VIGENCIA_HASTA_NULA);
        }
    }

    private void assertRangoFechasValido(Instant vigenteDesde, Instant vigenteHasta) {
        if (vigenteDesde.isAfter(vigenteHasta)) {
            throw new RuntimeException(ERROR_RANGO_FECHAS_INVALIDO);
        }
    }

    public BigDecimal montoAplicadoSobre(BigDecimal total, Instant fechaHora) {
        assertTotalNoNulo(total);
        assertTotalNoNegativo(total);
        assertFechaHoraNoNula(fechaHora);

        if (!aplicaEn(fechaHora)) {
            return BigDecimal.ZERO;
        }

        return total
                .multiply(porcentaje)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public boolean aplicaEn(Instant fechaHora) {
        assertFechaHoraNoNula(fechaHora);
        return !fechaHora.isBefore(vigenteDesde) && !fechaHora.isAfter(vigenteHasta);
    }

    private void assertTotalNoNulo(BigDecimal total) {
        if (total == null) {
            throw new RuntimeException(ERROR_TOTAL_NULO);
        }
    }

    private void assertTotalNoNegativo(BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(ERROR_TOTAL_NEGATIVO);
        }
    }

    private void assertFechaHoraNoNula(Instant fechaHora) {
        if (fechaHora == null) {
            throw new RuntimeException(ERROR_FECHA_HORA_NULA);
        }
    }
}