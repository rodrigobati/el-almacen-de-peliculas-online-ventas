package unrn.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PeliculaProyeccionRepositoryIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("almacen_ventas")
            .withUsername("almacen")
            .withPassword("almacen");

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driverClassName", mysql::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.flyway.schemas", () -> "almacen_ventas");
        registry.add("spring.flyway.default-schema", () -> "almacen_ventas");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PeliculaProyeccionRepository proyeccionRepository;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM pelicula_proyeccion");
    }

    @Test
    @DisplayName("InicioAplicacion flywayCreaTablaPeliculaProyeccion true")
    void inicioAplicacion_flywayCreaTablaPeliculaProyeccion_true() {
        // Setup: Preparar el escenario

        // Ejercitación: Ejecutar la acción a probar
        Integer cantidadTabla = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'pelicula_proyeccion'",
                Integer.class);
        List<String> columnas = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'pelicula_proyeccion' ORDER BY ordinal_position",
                String.class);

        // Verificación: Verificar el resultado esperado
        assertEquals(1, cantidadTabla, "La tabla pelicula_proyeccion debe existir en el esquema activo");
        assertEquals(
                List.of("movie_id", "titulo", "precio_actual", "activa", "version"),
                columnas,
                "La tabla pelicula_proyeccion debe exponer exactamente las columnas esperadas");
    }

    @Test
    @DisplayName("BuscarPorMovieId tablaConDatos devuelveProyeccion")
    void buscarPorMovieId_tablaConDatos_devuelveProyeccion() {
        // Setup: Preparar el escenario
        String movieId = "movie-123";
        jdbcTemplate.update(
                "INSERT INTO pelicula_proyeccion (movie_id, titulo, precio_actual, activa, version) VALUES (?, ?, ?, ?, ?)",
                movieId,
                "Matrix",
                new BigDecimal("100.00"),
                true,
                1L);

        // Ejercitación: Ejecutar la acción a probar
        var encontrada = proyeccionRepository.buscarPorMovieId(movieId);

        // Verificación: Verificar el resultado esperado
        assertTrue(encontrada.isPresent(), "La proyección debe existir para el movieId insertado");
        assertEquals(movieId, encontrada.get().movieId(), "El movieId debe coincidir");
        assertEquals("Matrix", encontrada.get().titulo(), "El título debe coincidir");
        assertEquals(new BigDecimal("100.00"), encontrada.get().precioActual(), "El precio actual debe coincidir");
        assertTrue(encontrada.get().activa(), "La película debe estar activa");
        assertEquals(1L, encontrada.get().version(), "La versión debe coincidir");
    }
}