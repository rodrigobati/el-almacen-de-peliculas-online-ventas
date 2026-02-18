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
@Table(name = "compra_item")
public class CompraItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private CompraEntity compra;

    @Column(name = "pelicula_id", nullable = false, length = 64)
    private String peliculaId;

    @Column(name = "titulo_al_comprar", nullable = false, length = 255)
    private String tituloAlComprar;

    @Column(name = "precio_al_comprar", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioAlComprar;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    protected CompraItemEntity() {
    }

    public CompraItemEntity(String peliculaId, String tituloAlComprar, BigDecimal precioAlComprar, int cantidad,
            BigDecimal subtotal) {
        this.peliculaId = peliculaId;
        this.tituloAlComprar = tituloAlComprar;
        this.precioAlComprar = precioAlComprar;
        this.cantidad = cantidad;
        this.subtotal = subtotal;
    }

    public void asociarACompra(CompraEntity compra) {
        this.compra = compra;
    }

    public String getPeliculaId() {
        return peliculaId;
    }

    public String getTituloAlComprar() {
        return tituloAlComprar;
    }

    public BigDecimal getPrecioAlComprar() {
        return precioAlComprar;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}