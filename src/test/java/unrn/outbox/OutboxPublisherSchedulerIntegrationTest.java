package unrn.outbox;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import unrn.event.compra.CompraConfirmadaPublisher;
import unrn.event.stock.StockValidationRequestedEvent;
import unrn.event.stock.StockValidationRequestedPublisher;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "ventas.outbox.scheduler.enabled=true",
    "ventas.outbox.retry.base-delay-ms=0",
    "spring.task.scheduling.enabled=false"
})
class OutboxPublisherSchedulerIntegrationTest {

    @Autowired
    private OutboxPublisherScheduler outboxPublisherScheduler;

    @Autowired
    private OutboxEventService outboxEventService;

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CompraConfirmadaPublisher compraConfirmadaPublisher;

    @MockitoBean
    private StockValidationRequestedPublisher stockValidationRequestedPublisher;

    @BeforeEach
    void beforeEach() {
        emf.getSchemaManager().truncate();
    }

    @Test
    @DisplayName("PublicarPendientes outboxConStockValidationRequestedEvent publicaEventoYMarcaPublished")
    void publicarPendientes_outboxConStockValidationRequestedEvent_publicaEventoYMarcaPublished() {
        // Setup: registrar un evento pendiente de validacion de stock
        StockValidationRequestedEvent event = new StockValidationRequestedEvent(
                UUID.randomUUID().toString(),
                99L,
                List.of(new StockValidationRequestedEvent.Item(1L, 2)),
                Instant.now());

        outboxEventService.registrarStockValidationRequested(99L, event);

        // Ejercitación: ejecutar scheduler de outbox
        outboxPublisherScheduler.publicarPendientes();

        // Verificación: publica con publisher correcto y actualiza estado
        verify(stockValidationRequestedPublisher, atLeastOnce()).publicarAhora(any(StockValidationRequestedEvent.class));
        verify(compraConfirmadaPublisher, never()).publicarAhora(any());

        Integer published = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE event_type='StockValidationRequestedEvent' AND status='PUBLISHED'",
                Integer.class);

        assertEquals(1, published,
                "El StockValidationRequestedEvent debe quedar publicado despues de ejecutar el scheduler");
    }
}
