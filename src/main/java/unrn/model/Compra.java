package unrn.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Compra {

    static final String ERROR_DETALLE_NULO = "El detalle no puede ser nulo";
    static final String ERROR_PELICULA_NULA = "La película no puede ser nula";

    private final List<DetalleCompra> detalles;

    public Compra() {
        this.detalles = new ArrayList<>();
    }

    public void agregarDetalleDesde(PeliculaProyeccion pelicula, int cantidad) {
        assertPeliculaNoNula(pelicula);
        var detalle = new DetalleCompra(
                pelicula.movieId(),
                pelicula.titulo(),
                pelicula.precioActual(),
                cantidad);
        agregarDetalle(detalle);
    }

    private void agregarDetalle(DetalleCompra detalle) {
        assertDetalleNoNulo(detalle);
        detalles.add(detalle);
    }

    private void assertDetalleNoNulo(DetalleCompra detalle) {
        if (detalle == null) {
            throw new RuntimeException(ERROR_DETALLE_NULO);
        }
    }

    private void assertPeliculaNoNula(PeliculaProyeccion pelicula) {
        if (pelicula == null) {
            throw new RuntimeException(ERROR_PELICULA_NULA);
        }
    }

    public List<DetalleCompra> detalles() {
        return Collections.unmodifiableList(detalles);
    }
}
