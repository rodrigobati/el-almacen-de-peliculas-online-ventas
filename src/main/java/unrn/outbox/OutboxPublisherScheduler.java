package unrn.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import unrn.event.compra.CompraConfirmadaEvent;
import unrn.event.compra.CompraConfirmadaPublisher;
import unrn.event.stock.StockValidationRequestedEvent;
import unrn.event.stock.StockValidationRequestedPublisher;

@Component
@ConditionalOnProperty(name = "ventas.outbox.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduler.class);

    private final OutboxEventService outboxEventService;
    private final CompraConfirmadaPublisher compraConfirmadaPublisher;
    private final StockValidationRequestedPublisher stockValidationRequestedPublisher;

    public OutboxPublisherScheduler(OutboxEventService outboxEventService,
            CompraConfirmadaPublisher compraConfirmadaPublisher,
            StockValidationRequestedPublisher stockValidationRequestedPublisher) {
        this.outboxEventService = outboxEventService;
        this.compraConfirmadaPublisher = compraConfirmadaPublisher;
        this.stockValidationRequestedPublisher = stockValidationRequestedPublisher;
    }

    @Scheduled(fixedDelayString = "${ventas.outbox.scheduler.delay-ms:3000}")
    public void publicarPendientes() {
        for (Long outboxId : outboxEventService.obtenerPendientesProcesables()) {
            try {
                String eventType = outboxEventService.obtenerTipoEvento(outboxId);
                if (OutboxEventService.EVENT_TYPE_COMPRA_CONFIRMADA.equals(eventType)) {
                    CompraConfirmadaEvent event = outboxEventService.leerEventoCompraConfirmada(outboxId);
                    compraConfirmadaPublisher.publicarAhora(event);
                } else if (OutboxEventService.EVENT_TYPE_STOCK_VALIDATION_REQUESTED.equals(eventType)) {
                    StockValidationRequestedEvent event = outboxEventService.leerEventoStockValidationRequested(outboxId);
                    stockValidationRequestedPublisher.publicarAhora(event);
                } else {
                    throw new RuntimeException("Tipo de evento outbox no soportado: " + eventType);
                }
                outboxEventService.marcarPublicado(outboxId);
            } catch (RuntimeException ex) {
                outboxEventService.registrarFallo(outboxId, ex.getMessage());
                log.error("Fallo publicación outbox id={} mensaje={}", outboxId, ex.getMessage());
            }
        }
    }
}
