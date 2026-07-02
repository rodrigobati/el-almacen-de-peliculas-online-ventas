package unrn.event.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import unrn.model.Carrito;
import unrn.repository.CarritoRepository;
import unrn.service.CompraAceptacionService;
import unrn.service.CompraCompensacionService;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false"
})
class CompraAceptacionIntegrationTest {

    private static final String ERROR_COMPRA_NO_ENCONTRADA_PARA_CONFIRMAR = "No se encontró la compra a confirmar";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private CompraAceptacionService compraAceptacionService;

        @Autowired
        private CompraCompensacionService compraCompensacionService;

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        emf.getSchemaManager().truncate();
        jdbcTemplate.update("DELETE FROM pelicula_proyeccion");
        new ResourceDatabasePopulator(new ClassPathResource("test-data.sql")).execute(dataSource);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("AceptarCompra eventoValido confirmaCompra y registraOutboxCompraConfirmada")
    void aceptarCompra_eventoValido_confirmaCompraYRegistraOutboxCompraConfirmada() throws Exception {
        // Setup: Crear compra en estado pendiente
        Carrito carrito = new Carrito();
        carrito.agregarPelicula("1", "Matrix", new BigDecimal("100.00"), 1);
        carritoRepository.guardar("cliente-aceptacion", carrito);

        String response = mockMvc.perform(post("/api/carrito/confirmar")
                .header("X-Cliente-Id", "cliente-aceptacion")
                .contentType(APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        Long compraId = json.get("compraId").asLong();
        String eventId = UUID.randomUUID().toString();
        StockValidationAcceptedEvent event = new StockValidationAcceptedEvent(eventId, compraId, Instant.now());

        // Ejercitación: aplicar aceptación dos veces (idempotencia)
        compraAceptacionService.aceptar(event);
        compraAceptacionService.aceptar(event);

        // Verificación: compra confirmada
        mockMvc.perform(get("/api/compras/{id}", compraId)
                .header("X-Cliente-Id", "cliente-aceptacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        Integer procesados = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_events WHERE event_id = ?",
                Integer.class,
                eventId);
        assertEquals(1, procesados, "El eventId debe persistirse una sola vez para garantizar idempotencia");

        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE aggregate_type='COMPRA' AND event_type='CompraConfirmadaEvent'",
                Integer.class);
        assertEquals(1, outboxCount,
                "Debe registrarse un único CompraConfirmadaEvent en outbox tras aceptación de stock");

        String payload = jdbcTemplate.queryForObject(
                "SELECT payload_json FROM outbox_event WHERE aggregate_type='COMPRA' AND event_type='CompraConfirmadaEvent'",
                String.class);
        assertNotNull(payload, "El payload de outbox para CompraConfirmadaEvent debe existir");
    }

    @Test
    @DisplayName("AceptarCompra compraInexistente lanzaRuntimeException")
    void aceptarCompra_compraInexistente_lanzaRuntimeException() {
        // Setup: evento con compra inexistente
        StockValidationAcceptedEvent event = new StockValidationAcceptedEvent(
                UUID.randomUUID().toString(),
                9999L,
                Instant.now());

        // Ejercitación y Verificación: debe fallar con mensaje esperado
        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> compraAceptacionService.aceptar(event));

        assertEquals(ERROR_COMPRA_NO_ENCONTRADA_PARA_CONFIRMAR, ex.getMessage(),
                "Debe informar que no existe la compra a confirmar");
    }

    @Test
    @DisplayName("AceptarCompra compraRechazada mantieneEstado y noRegistraCompraConfirmadaEvent")
    void aceptarCompra_compraRechazada_mantieneEstadoYNoRegistraCompraConfirmadaEvent() throws Exception {
        // Setup: crear compra pendiente y luego rechazarla
        Carrito carrito = new Carrito();
        carrito.agregarPelicula("1", "Matrix", new BigDecimal("100.00"), 1);
        carritoRepository.guardar("cliente-aceptacion-rechazada", carrito);

        String response = mockMvc.perform(post("/api/carrito/confirmar")
                .header("X-Cliente-Id", "cliente-aceptacion-rechazada")
                .contentType(APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        Long compraId = json.get("compraId").asLong();

        StockRechazadoEvent rechazo = new StockRechazadoEvent(
                UUID.randomUUID().toString(),
                compraId,
                "STOCK_INSUFICIENTE",
                java.util.List.of(new StockRechazadoEvent.DetalleStockRechazado(1L, 1, "0")));
        compraCompensacionService.compensar(rechazo);

        String acceptedEventId = UUID.randomUUID().toString();
        StockValidationAcceptedEvent accepted = new StockValidationAcceptedEvent(acceptedEventId, compraId, Instant.now());

        // Ejercitación: procesar aceptación sobre compra ya rechazada
        compraAceptacionService.aceptar(accepted);

        // Verificación: compra sigue rechazada y no se registra confirmación en outbox
        mockMvc.perform(get("/api/compras/{id}", compraId)
                .header("X-Cliente-Id", "cliente-aceptacion-rechazada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));

        Integer outboxCompraConfirmada = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE aggregate_type='COMPRA' AND event_type='CompraConfirmadaEvent'",
                Integer.class);
        assertEquals(0, outboxCompraConfirmada,
                "No debe registrarse CompraConfirmadaEvent cuando llega aceptación para una compra RECHAZADA");

        Integer aceptadoProcesado = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_events WHERE event_id = ?",
                Integer.class,
                acceptedEventId);
        assertEquals(1, aceptadoProcesado,
                "El evento accepted debe marcarse procesado para evitar reintentos infinitos");
    }
}
