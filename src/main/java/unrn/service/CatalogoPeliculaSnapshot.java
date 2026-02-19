package unrn.service;

import java.math.BigDecimal;

public class CatalogoPeliculaSnapshot {

    static final String ERROR_ID_NULO = "El id de película del catálogo no puede ser nulo";
    static final String ERROR_TITULO_NULO = "El título de película del catálogo no puede ser nulo";
    static final String ERROR_TITULO_VACIO = "El título de película del catálogo no puede estar vacío";
    static final String ERROR_PRECIO_NULO = "El precio de película del catálogo no puede ser nulo";
    static final String ERROR_PRECIO_INVALIDO = "El precio de película del catálogo debe ser mayor a cero";
    static final String ERROR_VERSION_INVALIDA = "La versión de película del catálogo debe ser mayor a cero";

    private final Long movieId;
    private final String titulo;
    private final BigDecimal precio;
    private final boolean activa;
    private final long version;

    public CatalogoPeliculaSnapshot(Long movieId, String titulo, BigDecimal precio, boolean activa, long version) {
        assertMovieIdNoNulo(movieId);
        assertTituloNoNulo(titulo);
        assertTituloNoVacio(titulo);
        assertPrecioNoNulo(precio);
        assertPrecioValido(precio);
        assertVersionValida(version);

        this.movieId = movieId;
        this.titulo = titulo;
        this.precio = precio;
        this.activa = activa;
        this.version = version;
    }

    private void assertMovieIdNoNulo(Long movieId) {
        if (movieId == null) {
            throw new RuntimeException(ERROR_ID_NULO);
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

    private void assertVersionValida(long version) {
        if (version <= 0) {
            throw new RuntimeException(ERROR_VERSION_INVALIDA);
        }
    }

    public Long movieId() {
        return movieId;
    }

    public String titulo() {
        return titulo;
    }

    public BigDecimal precio() {
        return precio;
    }

    public boolean activa() {
        return activa;
    }

    public long version() {
        return version;
    }
}
