package unrn.event.stock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StockValidationRequestedPublisher {

    static final String ERROR_PUBLICACION_STOCK_VALIDATION_REQUESTED =
            "No se pudo publicar el evento StockValidationRequested";

    private static final Logger log = LoggerFactory.getLogger(StockValidationRequestedPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange ventasEventsExchange;

    @Value("${rabbitmq.catalogo.stock.validation.requested.routing-key:catalogo.stock.validation.requested}")
    private String routingKey;

    public StockValidationRequestedPublisher(RabbitTemplate rabbitTemplate,
            @Qualifier("ventasEventsExchange") TopicExchange ventasEventsExchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.ventasEventsExchange = ventasEventsExchange;
    }

    public void publicarAhora(StockValidationRequestedEvent event) {
        try {
            log.info("Publicando StockValidationRequested eventId={} compraId={}", event.eventId(), event.compraId());
            rabbitTemplate.convertAndSend(
                    ventasEventsExchange.getName(),
                    routingKey,
                    event,
                    message -> {
                        message.getMessageProperties().setHeader("x-event-id", event.eventId());
                        message.getMessageProperties().setHeader("x-correlation-id", event.eventId());
                        message.getMessageProperties().setHeader("x-source", "ventas");
                        message.getMessageProperties().setHeader(AmqpHeaders.TYPE,
                                StockValidationRequestedEvent.EVENT_TYPE);
                        return message;
                    });
        } catch (RuntimeException ex) {
            log.error("No se pudo publicar StockValidationRequested eventId={} compraId={} mensaje={}",
                    event.eventId(), event.compraId(), ex.getMessage());
            throw new RuntimeException(ERROR_PUBLICACION_STOCK_VALIDATION_REQUESTED, ex);
        }
    }
}
