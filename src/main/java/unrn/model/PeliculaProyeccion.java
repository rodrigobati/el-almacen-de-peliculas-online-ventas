package unrn.model;

import java.math.BigDecimal;

public class PeliculaProyeccion {

    static final String ERROR_PELICULA_ID_NULO = "El id de la película no puede ser nulo";
    static final String ERROR_PELICULA_ID_VACIO = "El id de la película no puede estar vacío";
    static final String ERROR_TITULO_NULO = "El título no puede ser nulo";
    static final String ERROR_TITULO_VACIO = "El título no puede estar vacío";
    static final String ERROR_PRECIO_NULO = "El precio no puede ser nulo";
    static final String ERROR_PRECIO_INVALIDO = "El precio debe ser mayor a cero";
    static final String ERROR_VERSION_INVALIDA = "La versión debe ser mayor a cero";

    private final String movieId;
    private final String titulo;
    private final BigDecimal precioActual;
    private final boolean activa;
    private final long version;

    public PeliculaProyeccion(String movieId, String titulo, BigDecimal precioActual, boolean activa, long version) {
        assertMovieIdNoNulo(movieId);
        assertMovieIdNoVacio(movieId);
        assertTituloNoNulo(titulo);
        assertTituloNoVacio(titulo);
        assertPrecioNoNulo(precioActual);
        assertPrecioValido(precioActual);
        assertVersionValida(version);

        this.movieId = movieId;
        this.titulo = titulo;
        this.precioActual = precioActual;
        this.activa = activa;
        this.version = version;
    }

    private void assertMovieIdNoNulo(String movieId) {
        if (movieId == null) {
            throw new RuntimeException(ERROR_PELICULA_ID_NULO);
        }
    }

    private void assertMovieIdNoVacio(String movieId) {
        if (movieId.trim().isEmpty()) {
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

    private void assertPrecioNoNulo(BigDecimal precioActual) {
        if (precioActual == null) {
            throw new RuntimeException(ERROR_PRECIO_NULO);
        }
    }

    private void assertPrecioValido(BigDecimal precioActual) {
        if (precioActual.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(ERROR_PRECIO_INVALIDO);
        }
    }

    private void assertVersionValida(long version) {
        if (version <= 0) {
            throw new RuntimeException(ERROR_VERSION_INVALIDA);
        }
    }

    public String movieId() {
        return movieId;
    }

    public String titulo() {
        return titulo;
    }

    public BigDecimal precioActual() {
        return precioActual;
    }

    public boolean activa() {
        return activa;
    }

    public long version() {
        return version;
    }
}
