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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compra")
public class CompraEntity {

    static final String ERROR_COMPRA_YA_RECHAZADA = "La compra ya está rechazada";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false, length = 128)
    private String clienteId;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "descuento_aplicado", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuentoAplicado;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "estado", nullable = false, length = 32)
    private String estado;

    @Column(name = "motivo_rechazo", length = 64)
    private String motivoRechazo;

    @Column(name = "detalles_rechazo", length = 1000)
    private String detallesRechazo;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CompraItemEntity> items = new ArrayList<>();

    protected CompraEntity() {
    }

    public CompraEntity(String clienteId, Instant fechaHora, BigDecimal subtotal, BigDecimal descuentoAplicado,
            BigDecimal total) {
        this.clienteId = clienteId;
        this.fechaHora = LocalDateTime.ofInstant(fechaHora, ZoneOffset.UTC);
        this.subtotal = subtotal;
        this.descuentoAplicado = descuentoAplicado;
        this.total = total;
        this.estado = EstadoCompra.CONFIRMADA.name();
    }

    public void agregarItem(CompraItemEntity item) {
        item.asociarACompra(this);
        this.items.add(item);
    }

    public Long getId() {
        return id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public Instant getFechaHora() {
        return fechaHora.toInstant(ZoneOffset.UTC);
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDescuentoAplicado() {
        return descuentoAplicado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getEstado() {
        return estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public String getDetallesRechazo() {
        return detallesRechazo;
    }

    public List<CompraItemEntity> getItems() {
        return items;
    }

    public void rechazar(String motivo, String detalles) {
        if (EstadoCompra.RECHAZADA.name().equals(estado)) {
            return;
        }

        this.estado = EstadoCompra.RECHAZADA.name();
        this.motivoRechazo = motivo;
        this.detallesRechazo = detalles;
    }
}