package unrn.model;

import java.math.BigDecimal;

public class PeliculaEnCarrito {

    static final String ERROR_PELICULA_ID_NULO = "El id de la película no puede ser nulo";
    static final String ERROR_PELICULA_ID_VACIO = "El id de la película no puede estar vacío";
    static final String ERROR_TITULO_NULO = "El título no puede ser nulo";
    static final String ERROR_TITULO_VACIO = "El título no puede estar vacío";
    static final String ERROR_PRECIO_NULO = "El precio unitario no puede ser nulo";
    static final String ERROR_PRECIO_INVALIDO = "El precio unitario debe ser mayor a cero";
    static final String ERROR_CANTIDAD_INVALIDA = "La cantidad debe ser mayor a cero";
    static final String ERROR_DELTA_INVALIDO = "El delta para incrementar debe ser mayor a cero";
    static final String ERROR_PELICULA_DISTINTA = "No se puede absorber una película con diferente id";
    static final String ERROR_PELICULA_A_ABSORBER_NULA = "La película a absorber no puede ser nula";

    private final String peliculaId;
    private final String titulo;
    private final BigDecimal precioUnitario;
    private int cantidad;

    public PeliculaEnCarrito(String peliculaId, String titulo, BigDecimal precioUnitario, int cantidad) {
        assertPeliculaIdNoNulo(peliculaId);
        assertPeliculaIdNoVacio(peliculaId);
        assertTituloNoNulo(titulo);
        assertTituloNoVacio(titulo);
        assertPrecioUnitarioNoNulo(precioUnitario);
        assertPrecioUnitarioValido(precioUnitario);
        assertCantidadValida(cantidad);

        this.peliculaId = peliculaId;
        this.titulo = titulo;
        this.precioUnitario = precioUnitario;
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

    private void assertPrecioUnitarioNoNulo(BigDecimal precioUnitario) {
        if (precioUnitario == null) {
            throw new RuntimeException(ERROR_PRECIO_NULO);
        }
    }

    private void assertPrecioUnitarioValido(BigDecimal precioUnitario) {
        if (precioUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(ERROR_PRECIO_INVALIDO);
        }
    }

    private void assertCantidadValida(int cantidad) {
        if (cantidad <= 0) {
            throw new RuntimeException(ERROR_CANTIDAD_INVALIDA);
        }
    }

    public boolean correspondeA(String peliculaId) {
        return this.peliculaId.equals(peliculaId);
    }

    public boolean esMismaPelicula(PeliculaEnCarrito otra) {
        if (otra == null) {
            return false;
        }
        return this.peliculaId.equals(otra.peliculaId);
    }

    public void incrementarCantidad(int delta) {
        assertDeltaValido(delta);
        this.cantidad += delta;
    }

    public void decrementarCantidad(int delta) {
        assertDeltaValido(delta);
        this.cantidad -= delta;
    }

    private void assertDeltaValido(int delta) {
        if (delta <= 0) {
            throw new RuntimeException(ERROR_DELTA_INVALIDO);
        }
    }

    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public void absorber(PeliculaEnCarrito otro) {
        assertPeliculaAAborberNoNula(otro);
        assertMismaPelicula(otro);
        this.cantidad += otro.cantidad;
    }

    public String peliculaId() {
        return peliculaId;
    }

    public String titulo() {
        return titulo;
    }

    public BigDecimal precioUnitario() {
        return precioUnitario;
    }

    public int cantidad() {
        return cantidad;
    }

    private void assertPeliculaAAborberNoNula(PeliculaEnCarrito pelicula) {
        if (pelicula == null) {
            throw new RuntimeException(ERROR_PELICULA_A_ABSORBER_NULA);
        }
    }

    private void assertMismaPelicula(PeliculaEnCarrito otro) {
        if (!this.peliculaId.equals(otro.peliculaId)) {
            throw new RuntimeException(ERROR_PELICULA_DISTINTA);
        }
    }
}
