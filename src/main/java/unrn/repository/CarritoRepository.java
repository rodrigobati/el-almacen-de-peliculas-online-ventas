package unrn.repository;

import unrn.model.Carrito;

public interface CarritoRepository {

    Carrito obtenerDe(String clienteId);

    void guardar(String clienteId, Carrito carrito);
}
