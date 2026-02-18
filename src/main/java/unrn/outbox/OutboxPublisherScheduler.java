package unrn.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import unrn.event.compra.CompraConfirmadaEvent;
import unrn.event.compra.CompraConfirmadaPublisher;

@Component
@ConditionalOnProperty(name = "ventas.outbox.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduler.class);

    private final OutboxEventService outboxEventService;
    private final CompraConfirmadaPublisher compraConfirmadaPublisher;

    public OutboxPublisherScheduler(OutboxEventService outboxEventService,
            CompraConfirmadaPublisher compraConfirmadaPublisher) {
        this.outboxEventService = outboxEventService;
        this.compraConfirmadaPublisher = compraConfirmadaPublisher;
    }

    @Scheduled(fixedDelayString = "${ventas.outbox.scheduler.delay-ms:3000}")
    public void publicarPendientes() {
        for (Long outboxId : outboxEventService.obtenerPendientesProcesables()) {
            try {
                CompraConfirmadaEvent event = outboxEventService.leerEventoCompraConfirmada(outboxId);
                compraConfirmadaPublisher.publicarAhora(event);
                outboxEventService.marcarPublicado(outboxId);
            } catch (RuntimeException ex) {
                outboxEventService.registrarFallo(outboxId, ex.getMessage());
                log.error("Fallo publicación outbox id={} mensaje={}", outboxId, ex.getMessage());
            }
        }
    }
}
