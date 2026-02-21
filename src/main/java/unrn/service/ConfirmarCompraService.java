package unrn.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unrn.dto.CarritoCompraResponse;
import unrn.dto.CompraDetalleResponse;
import unrn.dto.CompraItemResponse;
import unrn.dto.CompraResumenResponse;
import unrn.dto.ConfirmarCompraRequest;
import unrn.dto.ConfirmarCompraResponse;
import unrn.dto.ItemCarritoCompraResponse;
import unrn.model.Carrito;
import unrn.model.Cliente;
import unrn.model.Compra;
import unrn.model.Descuento;
import unrn.model.DetalleCompra;
import unrn.model.PeliculaEnCarrito;
import unrn.event.compra.CompraConfirmadaEvent;
import unrn.outbox.OutboxEventService;
import unrn.persistence.CompraEntity;
import unrn.persistence.CompraItemEntity;
import unrn.persistence.CompraJpaRepository;
import unrn.repository.CarritoRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ConfirmarCompraService {

    static final String ERROR_FECHAS_DESCUENTO_INCOMPLETAS = "Para aplicar descuento debe informar vigenteDesde y vigenteHasta";
    static final String ERROR_COMPRA_NO_ENCONTRADA = "No se encontró la compra para el cliente autenticado";

    private final CarritoRepository carritoRepository;
    private final CompraJpaRepository compraJpaRepository;
    private final ClienteActualProvider clienteActualProvider;
    private final OutboxEventService outboxEventService;

    public ConfirmarCompraService(CarritoRepository carritoRepository,
            CompraJpaRepository compraJpaRepository,
            ClienteActualProvider clienteActualProvider,
            OutboxEventService outboxEventService) {
        this.carritoRepository = carritoRepository;
        this.compraJpaRepository = compraJpaRepository;
        this.clienteActualProvider = clienteActualProvider;
        this.outboxEventService = outboxEventService;
    }

    @Transactional(readOnly = true)
    public CarritoCompraResponse verCarrito() {
        String clienteId = clienteActualProvider.obtenerClienteId();
        Carrito carrito = carritoRepository.obtenerDe(clienteId);
        return mapearCarrito(carrito, BigDecimal.ZERO);
    }

    @Transactional
    public ConfirmarCompraResponse confirmarCompra(ConfirmarCompraRequest request) {
        String clienteId = clienteActualProvider.obtenerClienteId();
        Carrito carrito = carritoRepository.obtenerDe(clienteId);
        Instant ahora = Instant.now();

        Descuento descuento = construirDescuento(request);
        Compra compra = carrito.confirmarCompra(new Cliente(clienteId), ahora, descuento);

        CompraEntity compraEntity = mapearCompraAEntity(clienteId, compra);
        CompraEntity compraGuardada = compraJpaRepository.save(compraEntity);
        carritoRepository.guardar(clienteId, carrito);
        outboxEventService.registrarCompraConfirmada(compraGuardada.getId(), eventoDesde(compraGuardada));

        return new ConfirmarCompraResponse(
                compraGuardada.getId(),
                compraGuardada.getFechaHora(),
                compraGuardada.getTotal(),
                compraGuardada.getEstado());
    }

    @Transactional(readOnly = true)
    public List<CompraResumenResponse> historialCompras() {
        String clienteId = clienteActualProvider.obtenerClienteId();
        return compraJpaRepository.findByClienteIdOrderByFechaHoraDesc(clienteId)
                .stream()
                .map(compra -> new CompraResumenResponse(
                        compra.getId(),
                        compra.getFechaHora(),
                        compra.getTotal(),
                        compra.getEstado()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CompraDetalleResponse detalleCompra(Long compraId) {
        String clienteId = clienteActualProvider.obtenerClienteId();
        CompraEntity compra = compraJpaRepository.findByIdAndClienteId(compraId, clienteId)
                .orElseThrow(() -> new RuntimeException(ERROR_COMPRA_NO_ENCONTRADA));

        List<CompraItemResponse> items = compra.getItems()
                .stream()
                .map(item -> new CompraItemResponse(
                        item.getPeliculaId(),
                        item.getTituloAlComprar(),
                        item.getPrecioAlComprar(),
                        item.getCantidad(),
                        item.getSubtotal()))
                .toList();

        return new CompraDetalleResponse(
                compra.getId(),
                compra.getFechaHora(),
                compra.getSubtotal(),
                compra.getDescuentoAplicado(),
                compra.getTotal(),
                compra.getEstado(),
                compra.getMotivoRechazo(),
                compra.getDetallesRechazo(),
                items);
    }

    private CarritoCompraResponse mapearCarrito(Carrito carrito, BigDecimal descuentoAplicado) {
        List<ItemCarritoCompraResponse> items = new ArrayList<>();
        for (PeliculaEnCarrito item : carrito.items()) {
            items.add(new ItemCarritoCompraResponse(
                    item.peliculaId(),
                    item.titulo(),
                    item.precioUnitario(),
                    item.cantidad(),
                    item.subtotal()));
        }

        BigDecimal subtotal = carrito.total();
        BigDecimal totalFinal = subtotal.subtract(descuentoAplicado);

        return new CarritoCompraResponse(items, subtotal, descuentoAplicado, totalFinal);
    }

    private Descuento construirDescuento(ConfirmarCompraRequest request) {
        if (request == null || request.porcentajeDescuento() == null) {
            return Descuento.sinDescuento();
        }

        if (request.vigenteDesde() == null || request.vigenteHasta() == null) {
            throw new RuntimeException(ERROR_FECHAS_DESCUENTO_INCOMPLETAS);
        }

        return new Descuento(request.porcentajeDescuento(), request.vigenteDesde(), request.vigenteHasta());
    }

    private CompraEntity mapearCompraAEntity(String clienteId, Compra compra) {
        CompraEntity entity = new CompraEntity(
                clienteId,
                compra.fechaHoraCompra(),
                compra.subtotal(),
                compra.descuentoAplicado(),
                compra.total());

        for (DetalleCompra detalle : compra.detalles()) {
            entity.agregarItem(new CompraItemEntity(
                    detalle.peliculaId(),
                    detalle.tituloAlComprar(),
                    detalle.precioAlComprar(),
                    detalle.cantidad(),
                    detalle.subtotal()));
        }

        return entity;
    }

    private CompraConfirmadaEvent eventoDesde(CompraEntity compraGuardada) {
        List<CompraConfirmadaEvent.ItemCompraConfirmada> items = compraGuardada.getItems().stream()
                .map(item -> new CompraConfirmadaEvent.ItemCompraConfirmada(
                        item.getTituloAlComprar(),
                        item.getCantidad(),
                        item.getPrecioAlComprar()))
                .toList();

        var total = new CompraConfirmadaEvent.TotalCompraConfirmada(
                compraGuardada.getSubtotal(),
                compraGuardada.getDescuentoAplicado(),
                null);

        var data = new CompraConfirmadaEvent.Data(
                compraIdEstable(compraGuardada.getId()),
                compraGuardada.getClienteId(),
                compraGuardada.getFechaHora(),
                items,
                total);

        return new CompraConfirmadaEvent(data);
    }

    private UUID compraIdEstable(Long compraIdNumerico) {
        return UUID.nameUUIDFromBytes(("compra-" + compraIdNumerico).getBytes(StandardCharsets.UTF_8));
    }
}