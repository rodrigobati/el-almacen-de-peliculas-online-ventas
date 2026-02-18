package unrn.repository;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class PeliculaProyeccionRepositoryIntegrationTest {

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PeliculaProyeccionRepository proyeccionRepository;

    @BeforeEach
    void beforeEach() {
        emf.getSchemaManager().truncate();
    }

    @Test
    @DisplayName("InicioAplicacion tablaPeliculaProyeccionExiste true")
    void inicioAplicacion_tablaPeliculaProyeccionExiste_true() {
        // Setup: Preparar el escenario

        // Ejercitación: Ejecutar la acción a probar
        Integer cantidad = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = 'PELICULA_PROYECCION'",
                Integer.class);

        // Verificación: Verificar el resultado esperado
        assertEquals(1, cantidad, "La tabla pelicula_proyeccion debe existir en el esquema activo");
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