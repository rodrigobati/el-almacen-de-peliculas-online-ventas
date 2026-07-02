package unrn.event.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StockValidationRequestedPublisherTest {

    @Test
    @DisplayName("PublicarAhora usa ventasEventsExchange con routingKeyCatalogoStockValidationRequested")
    void publicarAhora_usaVentasEventsExchange_conRoutingKeyCatalogoStockValidationRequested() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        StockValidationRequestedPublisher publisher = new StockValidationRequestedPublisher(
                rabbitTemplate,
                new TopicExchange("ventas.events", true, false));
        ReflectionTestUtils.setField(publisher, "routingKey", "catalogo.stock.validation.requested");

        StockValidationRequestedEvent event = new StockValidationRequestedEvent(
                "event-1",
                4L,
                List.of(new StockValidationRequestedEvent.Item(4L, 1)),
                Instant.parse("2026-06-16T22:51:19Z"));

        publisher.publicarAhora(event);

        verify(rabbitTemplate).convertAndSend(
                eq("ventas.events"),
                eq("catalogo.stock.validation.requested"),
                same(event),
                any(MessagePostProcessor.class));
    }
}
