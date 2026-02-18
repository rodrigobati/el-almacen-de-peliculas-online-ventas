package unrn.model;

import java.math.BigDecimal;

public class DetalleCompra {

    static final String ERROR_PELICULA_ID_NULO = "El id de la película no puede ser nulo";
    static final String ERROR_PELICULA_ID_VACIO = "El id de la película no puede estar vacío";
    static final String ERROR_TITULO_NULO = "El título no puede ser nulo";
    static final String ERROR_TITULO_VACIO = "El título no puede estar vacío";
    static final String ERROR_PRECIO_NULO = "El precio no puede ser nulo";
    static final String ERROR_PRECIO_INVALIDO = "El precio debe ser mayor a cero";
    static final String ERROR_CANTIDAD_INVALIDA = "La cantidad debe ser mayor a cero";

    private final String peliculaId;
    private final String tituloAlComprar;
    private final BigDecimal precioAlComprar;
    private final int cantidad;

    public DetalleCompra(String peliculaId, String tituloAlComprar, BigDecimal precioAlComprar, int cantidad) {
        assertPeliculaIdNoNulo(peliculaId);
        assertPeliculaIdNoVacio(peliculaId);
        assertTituloNoNulo(tituloAlComprar);
        assertTituloNoVacio(tituloAlComprar);
        assertPrecioNoNulo(precioAlComprar);
        assertPrecioValido(precioAlComprar);
        assertCantidadValida(cantidad);

        this.peliculaId = peliculaId;
        this.tituloAlComprar = tituloAlComprar;
        this.precioAlComprar = precioAlComprar;
        this.cantidad = cantidad;
    }

    private void assertPeliculaIdNoNulo(String peliculaId) {
        if (peliculaId == null) {
            throw new RuntimeException(ERROR_PELICULA_ID_NULO);
        }
    }

    private void assertPeliculaIdNoVacio(String peliculaId) {
        if (peliculaId.trim().isEmpty()) {
            throw new RuntimeException(ERROR_PELICULA_ID_VACIO);
        }
    }

    private void assertTituloNoNulo(String titulo) {
        if (titulo == null) {
            throw new RuntimeException(ERROR_TITULO_NULO);
        }
    }

    private void assertTituloNoVacio(String titulo) {
        if (titulo.trim().isEmpty()) {
            throw new RuntimeException(ERROR_TITULO_VACIO);
        }
    }

    private void assertPrecioNoNulo(BigDecimal precio) {
        if (precio == null) {
            throw new RuntimeException(ERROR_PRECIO_NULO);
        }
    }

    private void assertPrecioValido(BigDecimal precio) {
        if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(ERROR_PRECIO_INVALIDO);
        }
    }

    private void assertCantidadValida(int cantidad) {
        if (cantidad <= 0) {
            throw new RuntimeException(ERROR_CANTIDAD_INVALIDA);
        }
    }

    public String peliculaId() {
        return peliculaId;
    }

    public String tituloAlComprar() {
        return tituloAlComprar;
    }

    public BigDecimal precioAlComprar() {
        return precioAlComprar;
    }

    public int cantidad() {
        return cantidad;
    }

    public BigDecimal subtotal() {
        return precioAlComprar.multiply(BigDecimal.valueOf(cantidad));
    }
}
