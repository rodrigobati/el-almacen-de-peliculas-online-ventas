package unrn.repository;

import org.springframework.stereotype.Repository;
import unrn.model.Carrito;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCarritoRepository implements CarritoRepository {

    private final Map<String, Carrito> carritos = new ConcurrentHashMap<>();

    @Override
    public Carrito obtenerDe(String clienteId) {
        return carritos.getOrDefault(clienteId, new Carrito());
    }

    @Override
    public void guardar(String clienteId, Carrito carrito) {
        carritos.put(clienteId, carrito);
    }
}
