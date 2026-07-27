package unrn.persistence.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import unrn.dto.CompraDetalleResponse;
import unrn.dto.CompraResumenResponse;
import unrn.persistence.CompraEntity;
import unrn.persistence.CompraItemEntity;

@Testcontainers
class CompraHistorialDocumentStoreIntegrationTest {

    @Container
    private static final GenericContainer<?> MONGO = new GenericContainer<>(DockerImageName.parse("mongo:7"))
            .withExposedPorts(27017);

    private MongoClient mongoClient;
    private MongoTemplate mongoTemplate;
    private CompraHistorialDocumentStore store;

    @BeforeEach
    void setUp() {
        String mongoUri = "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017);
        mongoClient = MongoClients.create(mongoUri);
        mongoTemplate = new MongoTemplate(mongoClient, "almacen_ventas_historial_test");
        mongoTemplate.dropCollection(CompraDocument.class);
        store = new CompraHistorialDocumentStore(provider(mongoTemplate), true);
    }

    @AfterEach
    void tearDown() {
        mongoClient.close();
    }

    @Test
    @DisplayName("RT-10: guarda y consulta el historial de compras en MongoDB")
    void guardaYConsultaHistorialEnMongoDb() {
        CompraEntity compra = new CompraEntity(
                "cliente-1",
                "cliente@example.com",
                Instant.parse("2026-07-27T15:00:00Z"),
                new BigDecimal("120.00"),
                new BigDecimal("20.00"),
                new BigDecimal("100.00"),
                "event-rt10");
        ReflectionTestUtils.setField(compra, "id", 42L);
        compra.agregarItem(new CompraItemEntity(
                "pelicula-1",
                "The Matrix",
                new BigDecimal("50.00"),
                2,
                new BigDecimal("100.00")));

        store.guardar(compra);

        List<CompraDocument> documentos = mongoTemplate.findAll(CompraDocument.class);
        assertEquals(1, documentos.size());
        assertEquals(42L, documentos.get(0).compraId());
        assertEquals("cliente-1", documentos.get(0).clienteId());

        List<CompraResumenResponse> historial = store.historialCompras("cliente-1");
        assertEquals(1, historial.size());
        assertEquals(42L, historial.get(0).compraId());
        assertEquals(new BigDecimal("100.00"), historial.get(0).totalFinal());

        CompraDetalleResponse detalle = store.detalleCompra(42L, "cliente-1").orElseThrow();
        assertEquals(42L, detalle.compraId());
        assertEquals(1, detalle.items().size());
        assertEquals("pelicula-1", detalle.items().get(0).peliculaId());
    }

    @Test
    @DisplayName("RT-10: no devuelve compras de otro cliente")
    void noDevuelveComprasDeOtroCliente() {
        CompraEntity compra = new CompraEntity(
                "cliente-1",
                Instant.parse("2026-07-27T15:00:00Z"),
                new BigDecimal("50.00"),
                BigDecimal.ZERO,
                new BigDecimal("50.00"),
                "event-rt10-otro-cliente");
        ReflectionTestUtils.setField(compra, "id", 43L);
        compra.agregarItem(new CompraItemEntity(
                "pelicula-2",
                "Inception",
                new BigDecimal("50.00"),
                1,
                new BigDecimal("50.00")));

        store.guardar(compra);

        assertTrue(store.historialCompras("cliente-2").isEmpty());
        assertTrue(store.detalleCompra(43L, "cliente-2").isEmpty());
    }

    private static ObjectProvider<MongoTemplate> provider(MongoTemplate mongoTemplate) {
        return new ObjectProvider<>() {
            @Override
            public MongoTemplate getObject() {
                return mongoTemplate;
            }

            @Override
            public MongoTemplate getIfAvailable() {
                return mongoTemplate;
            }
        };
    }
}
