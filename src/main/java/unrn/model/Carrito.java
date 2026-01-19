package main.java.unrn.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Carrito {

    static final String ERROR_ITEMS_NULOS = "La lista de items no puede ser nula";
    static final String ERROR_ITEM_NULO = "El item a agregar no puede ser nulo";
    static final String ERROR_PELICULA_ID_NULO = "El id de la película no puede ser nulo";
    static final String ERROR_ITEM_NO_ENCONTRADO = "No se encontró un item con el id de película especificado";

    private final List<ItemCarrito> items;

    public Carrito() {
        this.items = new ArrayList<>();
    }

    public Carrito(List<ItemCarrito> items) {
        assertItemsNoNulos(items);
        this.items = new ArrayList<>(items);
    }

    private void assertItemsNoNulos(List<ItemCarrito> items) {
        if (items == null) {
            throw new RuntimeException(ERROR_ITEMS_NULOS);
        }
    }

    public void agregarPelicula(ItemCarrito nuevoItem) {
        assertItemNoNulo(nuevoItem);

        ItemCarrito itemExistente = buscarItemPorPeliculaId(nuevoItem.peliculaId());

        if (itemExistente != null) {
            itemExistente.incrementarCantidad(nuevoItem.cantidad());
        } else {
            items.add(nuevoItem);
        }
    }

    private void assertItemNoNulo(ItemCarrito item) {
        if (item == null) {
            throw new RuntimeException(ERROR_ITEM_NULO);
        }
    }

    private ItemCarrito buscarItemPorPeliculaId(String peliculaId) {
        for (ItemCarrito item : items) {
            if (item.correspondeA(peliculaId)) {
                return item;
            }
        }
        return null;
    }

    public void eliminarPelicula(String peliculaId) {
        assertPeliculaIdNoNulo(peliculaId);

        ItemCarrito itemAEliminar = buscarItemPorPeliculaId(peliculaId);

        if (itemAEliminar == null) {
            throw new RuntimeException(ERROR_ITEM_NO_ENCONTRADO);
        }

        items.remove(itemAEliminar);
    }

    private void assertPeliculaIdNoNulo(String peliculaId) {
        if (peliculaId == null) {
            throw new RuntimeException(ERROR_PELICULA_ID_NULO);
        }
    }

    public BigDecimal total() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCarrito item : items) {
            total = total.add(item.subtotal());
        }
        return total;
    }

    public List<ItemCarrito> items() {
        return Collections.unmodifiableList(items);
    }
}
