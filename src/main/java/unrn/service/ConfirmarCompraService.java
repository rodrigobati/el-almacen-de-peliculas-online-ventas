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
import unrn.event.stock.StockValidationRequestedEvent;
import unrn.outbox.OutboxEventService;
import unrn.persistence.CompraEntity;
import unrn.persistence.CompraItemEntity;
import unrn.persistence.CompraJpaRepository;
import unrn.repository.CarritoRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ConfirmarCompraService {

    static final String ERROR_FECHAS_DESCUENTO_INCOMPLETAS = "Para aplicar descuento debe informar vigenteDesde y vigenteHasta";
    static final String ERROR_COMPRA_NO_ENCONTRADA = "No se encontró la compra para el cliente autenticado";
    static final String ERROR_CUPON_INVALIDO = "Cupón inválido";
    static final String ERROR_CUPON_RESPUESTA_INCOMPLETA = "Respuesta de descuentos incompleta";
        static final String MENSAJE_COMPRA_PENDIENTE_VALIDACION =
            "Compra registrada en estado PENDING. La validación de stock está en progreso";
        static final String ERROR_PELICULA_ID_INVALIDO_EN_EVENTO =
            "El peliculaId del evento de validación debe ser numérico";

    private final CarritoRepository carritoRepository;
    private final CompraJpaRepository compraJpaRepository;
    private final ClienteActualProvider clienteActualProvider;
    private final OutboxEventService outboxEventService;

    // NUEVO: cliente RPC a descuentos
    private final DescuentosRpcClient descuentosRpcClient;

    public ConfirmarCompraService(CarritoRepository carritoRepository,
                                  CompraJpaRepository compraJpaRepository,
                                  ClienteActualProvider clienteActualProvider,
                                  OutboxEventService outboxEventService,
                                  DescuentosRpcClient descuentosRpcClient) { // NUEVO parámetro
        this.carritoRepository = carritoRepository;
        this.compraJpaRepository = compraJpaRepository;
        this.clienteActualProvider = clienteActualProvider;
        this.outboxEventService = outboxEventService;
        this.descuentosRpcClient = descuentosRpcClient; // NUEVO
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
        String eventId = UUID.randomUUID().toString();

        CompraEntity compraEntity = mapearCompraAEntity(clienteId, compra, eventId);
        CompraEntity compraGuardada = compraJpaRepository.save(compraEntity);
        carritoRepository.guardar(clienteId, carrito);
        outboxEventService.registrarStockValidationRequested(compraGuardada.getId(),
            eventoStockDesde(compraGuardada));

        return new ConfirmarCompraResponse(
                compraGuardada.getId(),
                compraGuardada.getFechaHora(),
                compraGuardada.getTotal(),
            compraGuardada.getEstado(),
            MENSAJE_COMPRA_PENDIENTE_VALIDACION);
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

    // CAMBIO PRINCIPAL: si vino nombreCupon, pedir a descuentos el porcentaje+vigencia
    private Descuento construirDescuento(ConfirmarCompraRequest request) {
        if (request == null) {
            return Descuento.sinDescuento();
        }

        if (request.nombreCupon() != null && !request.nombreCupon().isBlank()) {
            var resp = descuentosRpcClient.validarCupon(request.nombreCupon());

            if (!resp.valido()) {
                throw new RuntimeException("CUPON_INVALIDO: " + resp.motivo());
            }

            if (resp.porcentajeDescuento() == null || resp.vigenteDesde() == null || resp.vigenteHasta() == null) {
                throw new RuntimeException("CUPON_RESPUESTA_INCOMPLETA");
            }

            ZoneId zone = ZoneId.systemDefault();

            Instant desde = resp.vigenteDesde()
                    .atStartOfDay(zone)
                    .toInstant();

            Instant hasta = resp.vigenteHasta()
                    .plusDays(1)
                    .atStartOfDay(zone)
                    .toInstant()
                    .minusNanos(1);

            return new Descuento(
                    BigDecimal.valueOf(resp.porcentajeDescuento()),
                    desde,
                    hasta);
        }

        // fallback: tu comportamiento actual
        if (request.porcentajeDescuento() == null) {
            return Descuento.sinDescuento();
        }
        if (request.vigenteDesde() == null || request.vigenteHasta() == null) {
            throw new RuntimeException(ERROR_FECHAS_DESCUENTO_INCOMPLETAS);
        }
        return new Descuento(request.porcentajeDescuento(), request.vigenteDesde(), request.vigenteHasta());
    }

    private CompraEntity mapearCompraAEntity(String clienteId, Compra compra, String eventId) {
        CompraEntity entity = new CompraEntity(
                clienteId,
                compra.fechaHoraCompra(),
                compra.subtotal(),
                compra.descuentoAplicado(),
                compra.total(),
                eventId);

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

    private StockValidationRequestedEvent eventoStockDesde(CompraEntity compraGuardada) {
        List<StockValidationRequestedEvent.Item> items = compraGuardada.getItems().stream()
                .map(item -> new StockValidationRequestedEvent.Item(
                        peliculaIdNumerico(item.getPeliculaId()),
                        item.getCantidad()))
                .toList();

        return new StockValidationRequestedEvent(
                compraGuardada.getEventId(),
                compraGuardada.getId(),
                items,
                Instant.now());
    }

    private Long peliculaIdNumerico(String peliculaId) {
        try {
            return Long.parseLong(peliculaId);
        } catch (NumberFormatException ex) {
            throw new RuntimeException(ERROR_PELICULA_ID_INVALIDO_EN_EVENTO);
        }
    }
}