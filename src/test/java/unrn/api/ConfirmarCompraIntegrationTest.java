package unrn.api;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import unrn.model.Carrito;
import unrn.repository.CarritoRepository;

import javax.sql.DataSource;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ConfirmarCompraIntegrationTest {

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private CarritoRepository carritoRepository;

        @Autowired
        private EntityManagerFactory emf;

        @Autowired
        private DataSource dataSource;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        private MockMvc mockMvc;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void beforeEach() {
                emf.getSchemaManager().truncate();
                jdbcTemplate.update("DELETE FROM pelicula_proyeccion");
                new ResourceDatabasePopulator(new ClassPathResource("test-data.sql")).execute(dataSource);
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }

        @Test
        @DisplayName("ConfirmarCompra carritoConItems creaCompraPendiente y vaciaCarrito")
        void confirmarCompra_carritoConItems_creaCompraPendienteYVaciaCarrito() throws Exception {
                // Setup: Preparar el escenario
                Carrito carrito = new Carrito();
                carrito.agregarPelicula("1", "Matrix", new BigDecimal("100.00"), 2);
                carritoRepository.guardar("cliente-1", carrito);

                // Ejercitación: Ejecutar la acción a probar
                mockMvc.perform(post("/api/carrito/confirmar")
                                .header("X-Cliente-Id", "cliente-1")
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                                // Verificación: Verificar el resultado esperado
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.compraId").isNumber())
                                .andExpect(jsonPath("$.fechaHora").isNotEmpty())
                                .andExpect(jsonPath("$.totalFinal").value(200.00))
                                .andExpect(jsonPath("$.estado").value("PENDING"))
                                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("PENDING")));

                mockMvc.perform(get("/api/carrito")
                                .header("X-Cliente-Id", "cliente-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.items.length()").value(0))
                                .andExpect(jsonPath("$.subtotal").value(0))
                                .andExpect(jsonPath("$.descuentoAplicado").value(0))
                                .andExpect(jsonPath("$.totalFinal").value(0));

                mockMvc.perform(get("/api/compras")
                                .header("X-Cliente-Id", "cliente-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].totalFinal").value(200.00))
                                .andExpect(jsonPath("$[0].estado").value("PENDING"));

                Integer outboxStockValidation = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM outbox_event WHERE aggregate_type='COMPRA' AND status='PENDING' AND event_type='StockValidationRequestedEvent'",
                                Integer.class);
                assertEquals(1, outboxStockValidation,
                                "Debe registrar un StockValidationRequestedEvent pendiente tras confirmar compra");

                Integer outboxCompraConfirmada = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM outbox_event WHERE aggregate_type='COMPRA' AND event_type='CompraConfirmadaEvent'",
                                Integer.class);
                assertEquals(0, outboxCompraConfirmada,
                                "No debe registrar CompraConfirmadaEvent en la confirmación inicial");

                String payloadJson = jdbcTemplate.queryForObject(
                                "SELECT payload_json FROM outbox_event WHERE event_type='StockValidationRequestedEvent'",
                                String.class);
                JsonNode payload = objectMapper.readTree(payloadJson);
                assertFalse(payload.has("data"), "El payload no debe incluir estructura de compra confirmada");
                assertEquals(4, payload.size(), "El payload debe tener solo eventId, compraId, items y occurredAt");
                assertEquals(true, payload.has("eventId"), "El payload debe incluir eventId");
                assertEquals(true, payload.has("compraId"), "El payload debe incluir compraId");
                assertEquals(true, payload.has("items"), "El payload debe incluir items");
                assertEquals(true, payload.has("occurredAt"), "El payload debe incluir occurredAt");
                assertEquals(2, payload.get("items").get(0).size(),
                                "Cada item debe incluir solo peliculaId y cantidad");
                assertEquals(true, payload.get("items").get(0).has("peliculaId"),
                                "Cada item debe incluir peliculaId");
                assertEquals(true, payload.get("items").get(0).has("cantidad"),
                                "Cada item debe incluir cantidad");
        }

        @Test
        @DisplayName("ConfirmarCompra carritoVacio retorna400")
        void confirmarCompra_carritoVacio_retorna400() throws Exception {
                // Setup: Preparar el escenario
                // carrito vacío para cliente-2

                // Ejercitación: Ejecutar la acción a probar
                mockMvc.perform(post("/api/carrito/confirmar")
                                .header("X-Cliente-Id", "cliente-2")
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                                // Verificación: Verificar el resultado esperado
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("CARRITO_VACIO"));
        }

        @Test
        @DisplayName("HistorialCompras devuelveSoloComprasDelCliente")
        void historialCompras_devuelveSoloComprasDelCliente() throws Exception {
                // Setup: Preparar el escenario
                guardarCarritoYConfirmar("cliente-a", "1", "Matrix", "100.00", 1);
                guardarCarritoYConfirmar("cliente-a", "2", "Inception", "120.00", 1);
                guardarCarritoYConfirmar("cliente-b", "1", "Matrix", "100.00", 1);

                // Ejercitación: Ejecutar la acción a probar
                mockMvc.perform(get("/api/compras")
                                .header("X-Cliente-Id", "cliente-a"))
                                // Verificación: Verificar el resultado esperado
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("DetalleCompra existente retornaDetalle")
        void detalleCompra_existente_retornaDetalle() throws Exception {
                // Setup: Preparar el escenario
                guardarCarritoYConfirmar("cliente-detalle", "1", "Matrix", "100.00", 1);

                ResultActions historial = mockMvc.perform(get("/api/compras")
                                .header("X-Cliente-Id", "cliente-detalle"));

                String body = historial.andReturn().getResponse().getContentAsString();
                JsonNode jsonNode = objectMapper.readTree(body);
                long compraId = jsonNode.get(0).get("compraId").asLong();

                // Ejercitación: Ejecutar la acción a probar
                mockMvc.perform(get("/api/compras/{id}", compraId)
                                .header("X-Cliente-Id", "cliente-detalle"))
                                // Verificación: Verificar el resultado esperado
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.compraId").value(compraId))
                                .andExpect(jsonPath("$.items.length()").value(1))
                                .andExpect(jsonPath("$.totalFinal").value(100.00));
        }

        @Test
        @DisplayName("DetalleCompra compraPendienteSinRechazo noExponeDatosDeRechazo")
        void detalleCompra_compraPendienteSinRechazo_noExponeDatosDeRechazo() throws Exception {
                // Setup: Preparar el escenario
                guardarCarritoYConfirmar("cliente-cthulhu", "32", "Call of Cthulhu", "1.00", 1);

                ResultActions historial = mockMvc.perform(get("/api/compras")
                                .header("X-Cliente-Id", "cliente-cthulhu"));

                String body = historial.andReturn().getResponse().getContentAsString();
                JsonNode jsonNode = objectMapper.readTree(body);
                long compraId = jsonNode.get(0).get("compraId").asLong();

                // Ejercitación: Ejecutar la acción a probar
                mockMvc.perform(get("/api/compras/{id}", compraId)
                                .header("X-Cliente-Id", "cliente-cthulhu"))
                                // Verificación: Verificar el resultado esperado
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.compraId").value(compraId))
                                .andExpect(jsonPath("$.estado").value("PENDING"))
                                .andExpect(jsonPath("$.motivoRechazo").value(org.hamcrest.Matchers.nullValue()))
                                .andExpect(jsonPath("$.detallesRechazo").value(org.hamcrest.Matchers.nullValue()));
        }

        private void guardarCarritoYConfirmar(String clienteId, String peliculaId, String titulo, String precio,
                        int cantidad)
                        throws Exception {
                Carrito carrito = new Carrito();
                carrito.agregarPelicula(peliculaId, titulo, new BigDecimal(precio), cantidad);
                carritoRepository.guardar(clienteId, carrito);

                int status = mockMvc.perform(post("/api/carrito/confirmar")
                                .header("X-Cliente-Id", clienteId)
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                                .andReturn()
                                .getResponse()
                                .getStatus();

                assertEquals(201, status, "La confirmación de compra en setup debe responder Created");
        }
}