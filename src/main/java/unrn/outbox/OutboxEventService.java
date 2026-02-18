package unrn.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import unrn.event.compra.CompraConfirmadaEvent;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxEventService {

    static final String AGGREGATE_TYPE_COMPRA = "COMPRA";
    static final String EVENT_TYPE_COMPRA_CONFIRMADA = "CompraConfirmadaEvent";
    static final String ERROR_SERIALIZACION_OUTBOX = "No se pudo serializar el evento para outbox";
    static final String ERROR_OUTBOX_NO_ENCONTRADO = "No se encontró evento outbox";

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${ventas.outbox.max-attempts:10}")
    private int maxAttempts;

    @Value("${ventas.outbox.retry.base-delay-ms:2000}")
    private long baseDelayMs;

    public OutboxEventService(
            OutboxEventJpaRepository outboxEventJpaRepository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public void registrarCompraConfirmada(Long compraId, CompraConfirmadaEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEventEntity entity = new OutboxEventEntity(
                    AGGREGATE_TYPE_COMPRA,
                    compraId,
                    EVENT_TYPE_COMPRA_CONFIRMADA,
                    payload);
            outboxEventJpaRepository.save(entity);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ERROR_SERIALIZACION_OUTBOX, ex);
        }
    }

    @Transactional(readOnly = true)
    public List<Long> obtenerPendientesProcesables() {
        Instant ahora = Instant.now();
        return outboxEventJpaRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING)
                .stream()
                .filter(evento -> debeProcesarseAhora(evento, ahora))
                .map(OutboxEventEntity::getId)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompraConfirmadaEvent leerEventoCompraConfirmada(Long outboxId) {
        OutboxEventEntity entity = outboxEventJpaRepository.findById(outboxId)
                .orElseThrow(() -> new RuntimeException(ERROR_OUTBOX_NO_ENCONTRADO));
        try {
            return objectMapper.readValue(entity.getPayloadJson(), CompraConfirmadaEvent.class);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("No se pudo deserializar payload outbox", ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarPublicado(Long outboxId) {
        OutboxEventEntity entity = outboxEventJpaRepository.findById(outboxId)
                .orElseThrow(() -> new RuntimeException(ERROR_OUTBOX_NO_ENCONTRADO));
        entity.marcarPublicado();
        meterRegistry.counter("ventas.outbox.published.total").increment();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(Long outboxId, String error) {
        OutboxEventEntity entity = outboxEventJpaRepository.findById(outboxId)
                .orElseThrow(() -> new RuntimeException(ERROR_OUTBOX_NO_ENCONTRADO));

        entity.registrarIntentoFallido(error, maxAttempts);
        meterRegistry.counter("ventas.outbox.publish.failed.total").increment();
        if (entity.getStatus() == OutboxEventStatus.FAILED) {
            meterRegistry.counter("ventas.outbox.failed.terminal.total").increment();
        }
    }

    private boolean debeProcesarseAhora(OutboxEventEntity evento, Instant ahora) {
        long backoffMs = (long) Math.pow(2, Math.max(0, evento.getAttempts())) * baseDelayMs;
        Instant disponibleDesde = evento.getCreatedAt().plusMillis(backoffMs);
        return !disponibleDesde.isAfter(ahora);
    }
}
