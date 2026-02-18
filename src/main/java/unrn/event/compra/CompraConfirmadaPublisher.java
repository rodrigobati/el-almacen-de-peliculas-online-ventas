package unrn.event.compra;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class CompraConfirmadaPublisher {

    static final String ERROR_PUBLICACION_COMPRA_CONFIRMADA = "No se pudo publicar el evento de compra confirmada";

    private static final Logger log = LoggerFactory.getLogger(CompraConfirmadaPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange ventasEventsExchange;
    private final MeterRegistry meterRegistry;

    @Value("${rabbitmq.ventas.compra.confirmada.routing-key}")
    private String routingKey;

    @Value("${rabbitmq.publisher.confirm-timeout-ms:5000}")
    private long confirmTimeoutMs;

    @Value("${rabbitmq.publisher.strict-confirms:true}")
    private boolean strictConfirms;

    public CompraConfirmadaPublisher(RabbitTemplate rabbitTemplate,
            @Qualifier("ventasEventsExchange") TopicExchange ventasEventsExchange,
            MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.ventasEventsExchange = ventasEventsExchange;
        this.meterRegistry = meterRegistry;
    }

    public void publicarAhora(CompraConfirmadaEvent event) {
        MDC.put("eventId", event.eventId());
        MDC.put("compraId", String.valueOf(event.compraId()));
        CorrelationData correlationData = new CorrelationData(event.eventId());
        try {
            log.info("Publicando CompraConfirmada eventId={} compraId={}", event.eventId(), event.compraId());
            rabbitTemplate.convertAndSend(
                    ventasEventsExchange.getName(),
                    routingKey,
                    event,
                    message -> {
                        message.getMessageProperties().setHeader("x-event-id", event.eventId());
                        message.getMessageProperties().setHeader("x-correlation-id", event.eventId());
                        message.getMessageProperties().setHeader("x-source", "ventas");
                        message.getMessageProperties().setHeader(AmqpHeaders.TYPE, "CompraConfirmadaEvent");
                        return message;
                    },
                    correlationData);
            validarConfirmacion(correlationData, event);
            meterRegistry.counter("ventas.publisher.compra_confirmada.success.total").increment();
        } catch (Exception ex) {
            log.error("No se pudo publicar CompraConfirmada eventId={} compraId={} mensaje={}",
                    event.eventId(), event.compraId(), ex.getMessage());
            meterRegistry.counter("ventas.publisher.compra_confirmada.failed.total").increment();
            if (strictConfirms) {
                throw new RuntimeException(ERROR_PUBLICACION_COMPRA_CONFIRMADA, ex);
            }
        } finally {
            MDC.remove("eventId");
            MDC.remove("compraId");
        }
    }

    private void validarConfirmacion(CorrelationData correlationData, CompraConfirmadaEvent event) throws Exception {
        if (!strictConfirms) {
            return;
        }

        CorrelationData.Confirm confirm = correlationData.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
        if (confirm == null || !confirm.isAck()) {
            String causa = confirm != null ? confirm.getReason() : "confirmación nula";
            throw new RuntimeException("Broker NACK para eventId=" + event.eventId() + " causa=" + causa);
        }

        if (correlationData.getReturned() != null) {
            throw new RuntimeException("Evento retornado por broker para eventId=" + event.eventId());
        }
    }
}
