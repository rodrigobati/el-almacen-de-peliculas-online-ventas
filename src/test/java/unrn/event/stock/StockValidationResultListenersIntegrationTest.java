package unrn.event.stock;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import unrn.service.CompraAceptacionService;
import unrn.service.CompraCompensacionService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false"
})
class StockValidationResultListenersIntegrationTest {

    @Autowired
    private StockValidationAcceptedListener stockValidationAcceptedListener;

    @Autowired
    private StockRechazadoListener stockRechazadoListener;

    @MockitoBean
    private CompraAceptacionService compraAceptacionService;

    @MockitoBean
    private CompraCompensacionService compraCompensacionService;

    @Test
    @DisplayName("StockValidationAcceptedListener eventoValido delegaServicio yAckeaMensaje")
    void stockValidationAcceptedListener_eventoValido_delegaServicioYAckeaMensaje() throws Exception {
        // Setup: evento accepted valido y mensaje de RabbitMQ
        StockValidationAcceptedEvent event = new StockValidationAcceptedEvent(
                UUID.randomUUID().toString(),
                10L,
                Instant.now());
        Message message = mensajeConDeliveryTag(1L);
        Channel channel = Mockito.mock(Channel.class);

        // Ejercitación: ejecutar listener
        stockValidationAcceptedListener.onStockValidationAccepted(event, message, channel);

        // Verificación: delega en servicio y confirma ACK
        verify(compraAceptacionService, times(1)).aceptar(event);
        verify(channel, times(1)).basicAck(1L, false);
    }

    @Test
    @DisplayName("StockRechazadoListener eventoValido delegaServicio yAckeaMensaje")
    void stockRechazadoListener_eventoValido_delegaServicioYAckeaMensaje() throws Exception {
        // Setup: evento rechazado valido y mensaje de RabbitMQ
        StockRechazadoEvent event = new StockRechazadoEvent(
                UUID.randomUUID().toString(),
                11L,
                "STOCK_INSUFICIENTE",
                List.of(new StockRechazadoEvent.DetalleStockRechazado(1L, 2, "0")));
        Message message = mensajeConDeliveryTag(2L);
        Channel channel = Mockito.mock(Channel.class);

        // Ejercitación: ejecutar listener
        stockRechazadoListener.onStockRechazado(event, message, channel);

        // Verificación: delega en servicio y confirma ACK
        verify(compraCompensacionService, times(1)).compensar(event);
        verify(channel, times(1)).basicAck(2L, false);
    }

    private Message mensajeConDeliveryTag(long deliveryTag) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message(new byte[0], properties);
    }
}
