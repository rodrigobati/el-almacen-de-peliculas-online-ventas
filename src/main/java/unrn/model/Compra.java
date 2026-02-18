package unrn.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Compra {

    static final String ERROR_CLIENTE_NULO = "El cliente no puede ser nulo";
    static final String ERROR_FECHA_HORA_NULA = "La fecha y hora de la compra no puede ser nula";
    static final String ERROR_DETALLES_NULOS = "La lista de detalles no puede ser nula";
    static final String ERROR_DETALLES_VACIOS = "La compra debe contener al menos un detalle";
    static final String ERROR_DETALLE_NULO = "La compra no puede contener detalles nulos";
    static final String ERROR_SUBTOTAL_NULO = "El subtotal no puede ser nulo";
    static final String ERROR_DESCUENTO_NULO = "El descuento aplicado no puede ser nulo";
    static final String ERROR_SUBTOTAL_INVALIDO = "El subtotal debe ser mayor a cero";
    static final String ERROR_DESCUENTO_NEGATIVO = "El descuento aplicado no puede ser negativo";
    static final String ERROR_DESCUENTO_MAYOR_A_SUBTOTAL = "El descuento aplicado no puede superar el subtotal";

    private final Cliente cliente;
    private final Instant fechaHoraCompra;
    private final List<DetalleCompra> detalles;
    private final BigDecimal subtotal;
    private final BigDecimal descuentoAplicado;
    private final BigDecimal total;

    public Compra(Cliente cliente,
            Instant fechaHoraCompra,
            List<DetalleCompra> detalles,
            BigDecimal subtotal,
            BigDecimal descuentoAplicado) {
        assertClienteNoNulo(cliente);
        assertFechaHoraNoNula(fechaHoraCompra);
        assertDetallesNoNulos(detalles);
        assertDetallesNoVacios(detalles);
        assertDetallesSinNulos(detalles);
        assertSubtotalNoNulo(subtotal);
        assertDescuentoNoNulo(descuentoAplicado);
        assertSubtotalValido(subtotal);
        assertDescuentoNoNegativo(descuentoAplicado);
        assertDescuentoNoMayorASubtotal(subtotal, descuentoAplicado);

        this.cliente = cliente;
        this.fechaHoraCompra = fechaHoraCompra;
        this.detalles = Collections.unmodifiableList(new ArrayList<>(detalles));
        this.subtotal = subtotal;
        this.descuentoAplicado = descuentoAplicado;
        this.total = subtotal.subtract(descuentoAplicado);
    }

    private void assertClienteNoNulo(Cliente cliente) {
        if (cliente == null) {
            throw new RuntimeException(ERROR_CLIENTE_NULO);
        }
    }

    private void assertFechaHoraNoNula(Instant fechaHoraCompra) {
        if (fechaHoraCompra == null) {
            throw new RuntimeException(ERROR_FECHA_HORA_NULA);
        }
    }

    private void assertDetallesNoNulos(List<DetalleCompra> detalles) {
        if (detalles == null) {
            throw new RuntimeException(ERROR_DETALLES_NULOS);
        }
    }

    private void assertDetallesNoVacios(List<DetalleCompra> detalles) {
        if (detalles.isEmpty()) {
            throw new RuntimeException(ERROR_DETALLES_VACIOS);
        }
    }

    private void assertDetallesSinNulos(List<DetalleCompra> detalles) {
        for (DetalleCompra detalle : detalles) {
            if (detalle == null) {
                throw new RuntimeException(ERROR_DETALLE_NULO);
            }
        }
    }

    private void assertSubtotalNoNulo(BigDecimal subtotal) {
        if (subtotal == null) {
            throw new RuntimeException(ERROR_SUBTOTAL_NULO);
        }
    }

    private void assertDescuentoNoNulo(BigDecimal descuentoAplicado) {
        if (descuentoAplicado == null) {
            throw new RuntimeException(ERROR_DESCUENTO_NULO);
        }
    }

    private void assertSubtotalValido(BigDecimal subtotal) {
        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(ERROR_SUBTOTAL_INVALIDO);
        }
    }

    private void assertDescuentoNoNegativo(BigDecimal descuentoAplicado) {
        if (descuentoAplicado.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(ERROR_DESCUENTO_NEGATIVO);
        }
    }

    private void assertDescuentoNoMayorASubtotal(BigDecimal subtotal, BigDecimal descuentoAplicado) {
        if (descuentoAplicado.compareTo(subtotal) > 0) {
            throw new RuntimeException(ERROR_DESCUENTO_MAYOR_A_SUBTOTAL);
        }
    }

    public Cliente cliente() {
        return cliente;
    }

    public Instant fechaHoraCompra() {
        return fechaHoraCompra;
    }

    public List<DetalleCompra> detalles() {
        return detalles;
    }

    public BigDecimal subtotal() {
        return subtotal;
    }

    public BigDecimal descuentoAplicado() {
        return descuentoAplicado;
    }

    public BigDecimal total() {
        return total;
    }
}
