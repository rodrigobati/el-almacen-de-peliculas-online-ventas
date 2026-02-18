package unrn.event.stock;

import com.rabbitmq.client.Channel;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import unrn.service.CompraCompensacionService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class StockRechazadoListener {

    private static final Logger log = LoggerFactory.getLogger(StockRechazadoListener.class);
    static final String ERROR_EVENTO_MALFORMADO = "Evento stock rechazado inválido";

    private final CompraCompensacionService compraCompensacionService;
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${rabbitmq.ventas.dlx.exchange}")
    private String deadLetterExchange;

    @Value("${rabbitmq.ventas.stock.rechazado.dlq.routing-key}")
    private String deadLetterRoutingKey;

    @Value("${rabbitmq.ventas.stock.rechazado.max-retries:3}")
    private int maxRetries;

    public StockRechazadoListener(CompraCompensacionService compraCompensacionService,
            RabbitTemplate rabbitTemplate,
            MeterRegistry meterRegistry) {
        this.compraCompensacionService = compraCompensacionService;
        this.rabbitTemplate = rabbitTemplate;
        this.meterRegistry = meterRegistry;
    }

    @RabbitListener(queues = "${rabbitmq.ventas.stock.rechazado.queue}", containerFactory = "manualAckRabbitListenerContainerFactory")
    public void onStockRechazado(StockRechazadoEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        MDC.put("eventId", event != null ? event.eventId() : "null");
        MDC.put("compraId", event != null ? String.valueOf(event.compraId()) : "null");

        try {
            assertEventoValido(event);

            compraCompensacionService.compensar(event);
            meterRegistry.counter("ventas.stock_rechazado.consumed.success.total").increment();
            channel.basicAck(deliveryTag, false);
            log.info("Compensacion aplicada para compraId={} por evento stock rechazado eventId={}",
                    event.compraId(), event.eventId());
        } catch (RuntimeException ex) {
            long reintentos = obtenerCantidadReintentos(message);

            if (reintentos >= maxRetries) {
                enviarADlq(message, ex.getMessage());
                channel.basicAck(deliveryTag, false);
                meterRegistry.counter("ventas.stock_rechazado.dlq.total").increment();
                log.error("Evento enviado a DLQ eventId={} compraId={} error={}",
                        event != null ? event.eventId() : "null",
                        event != null ? event.compraId() : null,
                        ex.getMessage());
            } else {
                channel.basicNack(deliveryTag, false, false);
                meterRegistry.counter("ventas.stock_rechazado.retry.total").increment();
                log.warn("Reintentando evento stock rechazado eventId={} compraId={} intentosPrevios={} error={}",
                        event != null ? event.eventId() : "null",
                        event != null ? event.compraId() : null,
                        reintentos,
                        ex.getMessage());
            }
        } finally {
            MDC.remove("eventId");
            MDC.remove("compraId");
        }
    }

    private void assertEventoValido(StockRechazadoEvent event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank() || event.compraId() == null
                || event.motivo() == null || event.motivo().isBlank()) {
            throw new RuntimeException(ERROR_EVENTO_MALFORMADO);
        }
    }

    private long obtenerCantidadReintentos(Message message) {
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
        if (!(xDeath instanceof List<?> entries) || entries.isEmpty()) {
            return 0;
        }

        Object first = entries.get(0);
        if (first instanceof Map<?, ?> deathEntry) {
            Object count = deathEntry.get("count");
            if (count instanceof Number number) {
                return number.longValue();
            }
        }

        return 0;
    }

    private void enviarADlq(Message original, String motivo) {
        Message dlqMessage = MessageBuilder.withBody(original.getBody())
                .andProperties(original.getMessageProperties())
                .setHeader("x-reject-reason", motivo)
                .build();

        rabbitTemplate.send(deadLetterExchange, deadLetterRoutingKey, dlqMessage);
    }
}
