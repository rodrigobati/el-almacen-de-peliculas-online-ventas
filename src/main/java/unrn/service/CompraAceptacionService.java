package unrn.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unrn.event.compra.CompraConfirmadaEvent;
import unrn.event.stock.StockValidationAcceptedEvent;
import unrn.outbox.OutboxEventService;
import unrn.persistence.CompraEntity;
import unrn.persistence.CompraJpaRepository;
import unrn.persistence.EstadoCompra;
import unrn.persistence.ProcessedEventEntity;
import unrn.persistence.ProcessedEventJpaRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CompraAceptacionService {

    static final String ERROR_COMPRA_NO_ENCONTRADA_PARA_CONFIRMAR = "No se encontró la compra a confirmar";
    static final String ERROR_EVENT_ID_REQUERIDO = "El eventId es obligatorio para idempotencia";

    private final CompraJpaRepository compraJpaRepository;
    private final ProcessedEventJpaRepository processedEventJpaRepository;
    private final OutboxEventService outboxEventService;
    private final MeterRegistry meterRegistry;

    public CompraAceptacionService(CompraJpaRepository compraJpaRepository,
            ProcessedEventJpaRepository processedEventJpaRepository,
            OutboxEventService outboxEventService,
            MeterRegistry meterRegistry) {
        this.compraJpaRepository = compraJpaRepository;
        this.processedEventJpaRepository = processedEventJpaRepository;
        this.outboxEventService = outboxEventService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public void aceptar(StockValidationAcceptedEvent event) {
        assertEventIdValido(event);
        if (processedEventJpaRepository.existsById(event.eventId())) {
            meterRegistry.counter("ventas.aceptacion.duplicados.ignorados.total").increment();
            return;
        }

        CompraEntity compra = compraJpaRepository.findById(event.compraId())
                .orElseThrow(() -> new RuntimeException(ERROR_COMPRA_NO_ENCONTRADA_PARA_CONFIRMAR));

        if (!EstadoCompra.PENDING.name().equals(compra.getEstado())) {
            processedEventJpaRepository.save(new ProcessedEventEntity(event.eventId()));
            meterRegistry.counter("ventas.aceptacion.no_pendiente.ignorada.total").increment();
            return;
        }

        compra.confirmar();
        outboxEventService.registrarCompraConfirmada(compra.getId(), crearEventoCompraConfirmada(compra, event));
        processedEventJpaRepository.save(new ProcessedEventEntity(event.eventId()));
        meterRegistry.counter("ventas.aceptacion.aplicada.total").increment();
    }

    private CompraConfirmadaEvent crearEventoCompraConfirmada(CompraEntity compra, StockValidationAcceptedEvent event) {
        List<CompraConfirmadaEvent.ItemCompraConfirmada> items = compra.getItems().stream()
                .map(item -> new CompraConfirmadaEvent.ItemCompraConfirmada(
                        item.getTituloAlComprar(),
                        item.getCantidad(),
                        item.getPrecioAlComprar()))
                .toList();

        CompraConfirmadaEvent.TotalCompraConfirmada total = new CompraConfirmadaEvent.TotalCompraConfirmada(
                compra.getSubtotal(),
                compra.getDescuentoAplicado(),
                descripcionDescuento(compra.getDescuentoAplicado()));

        CompraConfirmadaEvent.Data data = new CompraConfirmadaEvent.Data(
                uuidDesdeCompraId(compra.getId()),
                emailParaNotificacion(compra),
                event.occurredAt() != null ? event.occurredAt() : Instant.now(),
                items,
                total);

        return new CompraConfirmadaEvent(data);
    }

    private String emailParaNotificacion(CompraEntity compra) {
        String clienteEmail = compra.getClienteEmail();
        if (clienteEmail != null && !clienteEmail.isBlank()) {
            return clienteEmail;
        }
        return compra.getClienteId();
    }

    private String descripcionDescuento(BigDecimal descuento) {
        if (descuento == null || BigDecimal.ZERO.compareTo(descuento) == 0) {
            return "Sin descuento";
        }
        return "Descuento aplicado en compra";
    }

    private UUID uuidDesdeCompraId(Long compraId) {
        return UUID.nameUUIDFromBytes(String.valueOf(compraId).getBytes(StandardCharsets.UTF_8));
    }

    private void assertEventIdValido(StockValidationAcceptedEvent event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank()) {
            throw new RuntimeException(ERROR_EVENT_ID_REQUERIDO);
        }
    }
}
