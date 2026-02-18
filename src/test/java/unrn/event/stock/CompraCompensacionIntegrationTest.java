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
import unrn.service.CompraCompensacionService;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class CompraCompensacionIntegrationTest {

    private static final String ERROR_COMPRA_NO_ENCONTRADA_PARA_COMPENSAR = "No se encontró la compra a compensar";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CarritoRepository carritoRepository;

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
    @DisplayName("CompensarCompra rechazoDuplicado mantieneCompraRechazada sinErrores")
    void compensarCompra_rechazoDuplicado_mantieneCompraRechazadaSinErrores() throws Exception {
        // Setup: Crear compra confirmada
        Carrito carrito = new Carrito();
        carrito.agregarPelicula("1", "Matrix", new BigDecimal("100.00"), 1);
        carritoRepository.guardar("cliente-compensacion", carrito);

        String response = mockMvc.perform(post("/api/carrito/confirmar")
                .header("X-Cliente-Id", "cliente-compensacion")
                .contentType(APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        Long compraId = json.get("compraId").asLong();

        StockRechazadoEvent event = new StockRechazadoEvent(
                UUID.randomUUID().toString(),
                compraId,
                "STOCK_INSUFICIENTE",
                List.of(new StockRechazadoEvent.DetalleStockRechazado(1L, 1, "0")));

        // Ejercitación: aplicar compensación dos veces (idempotencia)
        compraCompensacionService.compensar(event);
        compraCompensacionService.compensar(event);

        // Verificación: estado final rechazado con motivo persistido
        mockMvc.perform(get("/api/compras/{id}", compraId)
                .header("X-Cliente-Id", "cliente-compensacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoRechazo").value("STOCK_INSUFICIENTE"))
                .andExpect(jsonPath("$.detallesRechazo").value(org.hamcrest.Matchers.containsString("peliculaId=1")));

        Integer procesados = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_events WHERE event_id = ?",
                Integer.class,
                event.eventId());
        assertEquals(1, procesados, "El eventId debe persistirse una sola vez para garantizar idempotencia");
    }

    @Test
    @DisplayName("CompensarCompra compraInexistente lanzaRuntimeException")
    void compensarCompra_compraInexistente_lanzaRuntimeException() {
        // Setup: evento con compra inexistente
        StockRechazadoEvent event = new StockRechazadoEvent(
                UUID.randomUUID().toString(),
                9999L,
                "PELICULA_INEXISTENTE",
                List.of());

        // Ejercitación y Verificación: debe fallar con mensaje esperado
        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> compraCompensacionService.compensar(event));

        assertEquals(ERROR_COMPRA_NO_ENCONTRADA_PARA_COMPENSAR, ex.getMessage(),
                "Debe informar que no existe la compra a compensar");
    }
}
