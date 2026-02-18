package unrn.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Carrito {

    static final String ERROR_ITEMS_NULOS = "La lista de items no puede ser nula";
    static final String ERROR_ITEM_NULO = "El item no puede ser nulo";
    static final String ERROR_PELICULA_ID_NULO = "El id de la película no puede ser nulo";
    static final String ERROR_PELICULA_ID_VACIO = "El id de la película no puede estar vacío";
    static final String ERROR_CANTIDAD_INVALIDA = "La cantidad debe ser mayor a cero";
    static final String ERROR_ITEM_NO_ENCONTRADO = "No se encontró un item con el id de película especificado";
    static final String ERROR_PELICULAS_DUPLICADAS = "No se permiten películas duplicadas en el carrito";
    static final String ERROR_CLIENTE_NULO = "El cliente no puede ser nulo";
    static final String ERROR_FECHA_HORA_NULA = "La fecha y hora no puede ser nula";
    static final String ERROR_DESCUENTO_NULO = "El descuento no puede ser nulo";
    static final String ERROR_CARRITO_VACIO = "No se puede confirmar una compra con carrito vacío";

    private final List<PeliculaEnCarrito> items;

    public Carrito() {
        this.items = new ArrayList<>();
    }

    public Carrito(List<PeliculaEnCarrito> items) {
        assertItemsNoNulos(items);
        assertItemsSinNulos(items);
        assertSinDuplicados(items);
        this.items = new ArrayList<>(items);
    }

    private void assertItemsNoNulos(List<PeliculaEnCarrito> items) {
        if (items == null) {
            throw new RuntimeException(ERROR_ITEMS_NULOS);
        }
    }

    private void assertItemsSinNulos(List<PeliculaEnCarrito> items) {
        for (PeliculaEnCarrito item : items) {
            if (item == null) {
                throw new RuntimeException(ERROR_ITEM_NULO);
            }
        }
    }

    private void assertSinDuplicados(List<PeliculaEnCarrito> items) {
        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                PeliculaEnCarrito item1 = items.get(i);
                PeliculaEnCarrito item2 = items.get(j);
                if (sonMismaPelicula(item1, item2)) {
                    throw new RuntimeException(ERROR_PELICULAS_DUPLICADAS);
                }
            }
        }
    }

    private boolean sonMismaPelicula(PeliculaEnCarrito item1, PeliculaEnCarrito item2) {
        return item1.esMismaPelicula(item2);
    }

    public void agregarPelicula(String peliculaId, String titulo, BigDecimal precioUnitario, int cantidad) {
        assertPeliculaIdNoNulo(peliculaId);
        assertPeliculaIdNoVacio(peliculaId);
        assertCantidadValida(cantidad);

        PeliculaEnCarrito itemExistente = buscarItemPorPeliculaId(peliculaId);

        if (itemExistente != null) {
            itemExistente.incrementarCantidad(cantidad);
        } else {
            PeliculaEnCarrito nuevaPelicula = new PeliculaEnCarrito(peliculaId, titulo, precioUnitario, cantidad);
            items.add(nuevaPelicula);
        }
    }

    private void assertCantidadValida(int cantidad) {
        if (cantidad <= 0) {
            throw new RuntimeException(ERROR_CANTIDAD_INVALIDA);
        }
    }

    private PeliculaEnCarrito buscarItemPorPeliculaId(String peliculaId) {
        for (PeliculaEnCarrito item : items) {
            if (item.correspondeA(peliculaId)) {
                return item;
            }
        }
        return null;
    }

    public void eliminarPelicula(String peliculaId) {
        assertPeliculaIdNoNulo(peliculaId);
        assertPeliculaIdNoVacio(peliculaId);

        PeliculaEnCarrito itemAEliminar = buscarItemPorPeliculaId(peliculaId);

        if (itemAEliminar == null) {
            throw new RuntimeException(ERROR_ITEM_NO_ENCONTRADO);
        }

        items.remove(itemAEliminar);
    }

    public void decrementarPelicula(String peliculaId) {
        assertPeliculaIdNoNulo(peliculaId);
        assertPeliculaIdNoVacio(peliculaId);

        PeliculaEnCarrito item = buscarItemPorPeliculaId(peliculaId);

        if (item == null) {
            throw new RuntimeException(ERROR_ITEM_NO_ENCONTRADO);
        }

        if (item.cantidad() <= 1) {
            items.remove(item);
            return;
        }

        item.decrementarCantidad(1);
    }

    public int cantidadDe(String peliculaId) {
        assertPeliculaIdNoNulo(peliculaId);
        assertPeliculaIdNoVacio(peliculaId);

        PeliculaEnCarrito item = buscarItemPorPeliculaId(peliculaId);
        return item == null ? 0 : item.cantidad();
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

    public BigDecimal total() {
        BigDecimal total = BigDecimal.ZERO;
        for (PeliculaEnCarrito item : items) {
            total = total.add(item.subtotal());
        }
        return total;
    }

    public List<PeliculaEnCarrito> items() {
        return Collections.unmodifiableList(items);
    }

    public Compra confirmarCompra(Cliente cliente, Instant fechaHora) {
        return confirmarCompra(cliente, fechaHora, Descuento.sinDescuento());
    }

    public Compra confirmarCompra(Cliente cliente, Instant fechaHora, Descuento descuento) {
        assertClienteNoNulo(cliente);
        assertFechaHoraNoNula(fechaHora);
        assertDescuentoNoNulo(descuento);
        assertCarritoNoVacio();

        BigDecimal subtotal = total();
        BigDecimal descuentoAplicado = descuento.montoAplicadoSobre(subtotal, fechaHora);
        List<DetalleCompra> detalles = construirDetallesCompra();

        Compra compra = new Compra(cliente, fechaHora, detalles, subtotal, descuentoAplicado);
        vaciar();
        return compra;
    }

    private void assertClienteNoNulo(Cliente cliente) {
        if (cliente == null) {
            throw new RuntimeException(ERROR_CLIENTE_NULO);
        }
    }

    private void assertFechaHoraNoNula(Instant fechaHora) {
        if (fechaHora == null) {
            throw new RuntimeException(ERROR_FECHA_HORA_NULA);
        }
    }

    private void assertDescuentoNoNulo(Descuento descuento) {
        if (descuento == null) {
            throw new RuntimeException(ERROR_DESCUENTO_NULO);
        }
    }

    private void assertCarritoNoVacio() {
        if (items.isEmpty()) {
            throw new RuntimeException(ERROR_CARRITO_VACIO);
        }
    }

    private List<DetalleCompra> construirDetallesCompra() {
        List<DetalleCompra> detalles = new ArrayList<>();
        for (PeliculaEnCarrito item : items) {
            detalles.add(new DetalleCompra(item.peliculaId(), item.titulo(), item.precioUnitario(), item.cantidad()));
        }
        return detalles;
    }

    private void vaciar() {
        items.clear();
    }
}
