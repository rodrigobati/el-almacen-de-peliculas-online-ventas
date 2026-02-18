package unrn.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carrito")
public class CarritoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false, unique = true, length = 128)
    private String clienteId;

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CarritoItemEntity> items = new ArrayList<>();

    protected CarritoEntity() {
    }

    public CarritoEntity(String clienteId) {
        this.clienteId = clienteId;
    }

    public void reemplazarItems(List<CarritoItemEntity> nuevosItems) {
        this.items.clear();
        for (CarritoItemEntity item : nuevosItems) {
            item.asociarACarrito(this);
            this.items.add(item);
        }
    }

    public Long getId() {
        return id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public List<CarritoItemEntity> getItems() {
        return items;
    }
}