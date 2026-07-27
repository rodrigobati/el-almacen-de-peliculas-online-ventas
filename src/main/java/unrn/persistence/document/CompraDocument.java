package unrn.persistence.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import unrn.dto.CompraDetalleResponse;
import unrn.dto.CompraItemResponse;
import unrn.dto.CompraResumenResponse;
import unrn.persistence.CompraEntity;

@Document(collection = "compras_historial")
@CompoundIndex(name = "idx_cliente_fecha", def = "{'clienteId': 1, 'fechaHora': -1}")
public class CompraDocument {

    @Id
    private String id;
    private Long compraId;
    private String clienteId;
    private String clienteEmail;
    private Instant fechaHora;
    private BigDecimal subtotal;
    private BigDecimal descuentoAplicado;
    private BigDecimal total;
    private String estado;
    private String motivoRechazo;
    private String detallesRechazo;
    private List<Item> items;

    protected CompraDocument() {
    }

    public CompraDocument(Long compraId,
            String clienteId,
            String clienteEmail,
            Instant fechaHora,
            BigDecimal subtotal,
            BigDecimal descuentoAplicado,
            BigDecimal total,
            String estado,
            String motivoRechazo,
            String detallesRechazo,
            List<Item> items) {
        this.id = String.valueOf(compraId);
        this.compraId = compraId;
        this.clienteId = clienteId;
        this.clienteEmail = clienteEmail;
        this.fechaHora = fechaHora;
        this.subtotal = subtotal;
        this.descuentoAplicado = descuentoAplicado;
        this.total = total;
        this.estado = estado;
        this.motivoRechazo = motivoRechazo;
        this.detallesRechazo = detallesRechazo;
        this.items = List.copyOf(items);
    }

    public static CompraDocument desde(CompraEntity compra) {
        List<Item> items = compra.getItems().stream()
                .map(item -> new Item(
                        item.getPeliculaId(),
                        item.getTituloAlComprar(),
                        item.getPrecioAlComprar(),
                        item.getCantidad(),
                        item.getSubtotal()))
                .toList();

        return new CompraDocument(
                compra.getId(),
                compra.getClienteId(),
                compra.getClienteEmail(),
                compra.getFechaHora(),
                compra.getSubtotal(),
                compra.getDescuentoAplicado(),
                compra.getTotal(),
                compra.getEstado(),
                compra.getMotivoRechazo(),
                compra.getDetallesRechazo(),
                items);
    }

    public CompraResumenResponse toResumenResponse() {
        return new CompraResumenResponse(compraId, fechaHora, total, estado);
    }

    public CompraDetalleResponse toDetalleResponse() {
        List<CompraItemResponse> itemResponses = items.stream()
                .map(Item::toResponse)
                .toList();

        return new CompraDetalleResponse(
                compraId,
                fechaHora,
                subtotal,
                descuentoAplicado,
                total,
                estado,
                motivoRechazo,
                detallesRechazo,
                itemResponses);
    }

    public Long compraId() {
        return compraId;
    }

    public String clienteId() {
        return clienteId;
    }

    public Instant fechaHora() {
        return fechaHora;
    }

    public static class Item {
        private String peliculaId;
        private String titulo;
        private BigDecimal precioAlComprar;
        private int cantidad;
        private BigDecimal subtotal;

        protected Item() {
        }

        public Item(String peliculaId,
                String titulo,
                BigDecimal precioAlComprar,
                int cantidad,
                BigDecimal subtotal) {
            this.peliculaId = peliculaId;
            this.titulo = titulo;
            this.precioAlComprar = precioAlComprar;
            this.cantidad = cantidad;
            this.subtotal = subtotal;
        }

        CompraItemResponse toResponse() {
            return new CompraItemResponse(peliculaId, titulo, precioAlComprar, cantidad, subtotal);
        }
    }
}