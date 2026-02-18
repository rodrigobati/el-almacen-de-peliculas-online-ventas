package unrn.repository;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import unrn.model.Carrito;
import unrn.model.PeliculaEnCarrito;
import unrn.persistence.CarritoEntity;
import unrn.persistence.CarritoItemEntity;
import unrn.persistence.CarritoJpaRepository;

import java.util.ArrayList;
import java.util.List;

@Repository
@Primary
public class JpaCarritoRepository implements CarritoRepository {

    private final CarritoJpaRepository carritoJpaRepository;

    public JpaCarritoRepository(CarritoJpaRepository carritoJpaRepository) {
        this.carritoJpaRepository = carritoJpaRepository;
    }

    @Override
    public Carrito obtenerDe(String clienteId) {
        return carritoJpaRepository.findByClienteId(clienteId)
                .map(this::aDominio)
                .orElseGet(Carrito::new);
    }

    @Override
    public void guardar(String clienteId, Carrito carrito) {
        CarritoEntity entity = carritoJpaRepository.findByClienteId(clienteId)
                .orElse(new CarritoEntity(clienteId));

        entity.reemplazarItems(aItemsEntity(carrito.items()));
        carritoJpaRepository.save(entity);
    }

    private Carrito aDominio(CarritoEntity entity) {
        List<PeliculaEnCarrito> items = new ArrayList<>();
        for (CarritoItemEntity item : entity.getItems()) {
            items.add(new PeliculaEnCarrito(
                    item.getPeliculaId(),
                    item.getTitulo(),
                    item.getPrecioUnitario(),
                    item.getCantidad()));
        }
        return new Carrito(items);
    }

    private List<CarritoItemEntity> aItemsEntity(List<PeliculaEnCarrito> items) {
        List<CarritoItemEntity> entities = new ArrayList<>();
        for (PeliculaEnCarrito item : items) {
            entities.add(new CarritoItemEntity(
                    item.peliculaId(),
                    item.titulo(),
                    item.precioUnitario(),
                    item.cantidad()));
        }
        return entities;
    }
}