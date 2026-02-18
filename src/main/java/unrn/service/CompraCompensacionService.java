package unrn.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unrn.event.stock.StockRechazadoEvent;
import unrn.persistence.CompraEntity;
import unrn.persistence.CompraJpaRepository;
import unrn.persistence.ProcessedEventEntity;
import unrn.persistence.ProcessedEventJpaRepository;

@Service
public class CompraCompensacionService {

    static final String ERROR_COMPRA_NO_ENCONTRADA_PARA_COMPENSAR = "No se encontró la compra a compensar";
    static final String ERROR_EVENT_ID_REQUERIDO = "El eventId es obligatorio para idempotencia";

    private final CompraJpaRepository compraJpaRepository;
    private final ProcessedEventJpaRepository processedEventJpaRepository;
    private final MeterRegistry meterRegistry;

    public CompraCompensacionService(CompraJpaRepository compraJpaRepository,
            ProcessedEventJpaRepository processedEventJpaRepository,
            MeterRegistry meterRegistry) {
        this.compraJpaRepository = compraJpaRepository;
        this.processedEventJpaRepository = processedEventJpaRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public void compensar(StockRechazadoEvent event) {
        assertEventIdValido(event);
        if (processedEventJpaRepository.existsById(event.eventId())) {
            meterRegistry.counter("ventas.compensacion.duplicados.ignorados.total").increment();
            return;
        }

        CompraEntity compra = compraJpaRepository.findById(event.compraId())
                .orElseThrow(() -> new RuntimeException(ERROR_COMPRA_NO_ENCONTRADA_PARA_COMPENSAR));

        String detalles = serializarDetalles(event);
        compra.rechazar(event.motivo(), detalles);
        processedEventJpaRepository.save(new ProcessedEventEntity(event.eventId()));
        meterRegistry.counter("ventas.compensacion.aplicada.total").increment();
    }

    private void assertEventIdValido(StockRechazadoEvent event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank()) {
            throw new RuntimeException(ERROR_EVENT_ID_REQUERIDO);
        }
    }

    private String serializarDetalles(StockRechazadoEvent event) {
        if (event.detalles() == null || event.detalles().isEmpty()) {
            return null;
        }

        StringBuilder detalles = new StringBuilder();
        for (StockRechazadoEvent.DetalleStockRechazado detalle : event.detalles()) {
            if (!detalles.isEmpty()) {
                detalles.append(" | ");
            }
            detalles.append("peliculaId=")
                    .append(detalle.peliculaId())
                    .append(", solicitado=")
                    .append(detalle.solicitado())
                    .append(", disponible=")
                    .append(detalle.disponible());
        }

        return detalles.toString();
    }
}
