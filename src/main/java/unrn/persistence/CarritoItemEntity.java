package unrn.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "carrito_item")
public class CarritoItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrito_id", nullable = false)
    private CarritoEntity carrito;

    @Column(name = "pelicula_id", nullable = false, length = 64)
    private String peliculaId;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    protected CarritoItemEntity() {
    }

    public CarritoItemEntity(String peliculaId, String titulo, BigDecimal precioUnitario, int cantidad) {
        this.peliculaId = peliculaId;
        this.titulo = titulo;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
    }

    public void asociarACarrito(CarritoEntity carrito) {
        this.carrito = carrito;
    }

    public String getPeliculaId() {
        return peliculaId;
    }

    public String getTitulo() {
        return titulo;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public int getCantidad() {
        return cantidad;
    }
}