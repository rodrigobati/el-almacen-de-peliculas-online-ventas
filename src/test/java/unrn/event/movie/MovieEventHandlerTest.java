package unrn.event.movie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import unrn.model.PeliculaProyeccion;
import unrn.repository.PeliculaProyeccionRepository;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class MovieEventHandlerTest {

    @Test
    @DisplayName("version cero sin proyeccion existente se normaliza a uno")
    void versionCero_sinProyeccionExistente_seNormalizaAUno() {
        // Setup: Preparar el escenario
        var repo = new InMemoryPeliculaProyeccionRepository();
        var handler = new MovieEventHandler(repo);
        var payload = new MovieEventPayload(1L, "Matrix", 1000.0, true, 0L);
        var event = new MovieEventEnvelope("evt-1", "MovieCreated.v1", java.time.Instant.now(), payload);

        // Ejercitación: Ejecutar la acción a probar
        handler.handle(event);

        // Verificación: Verificar el resultado esperado
        var proyeccion = repo.buscarPorMovieId("1");
        assertTrue(proyeccion.isPresent(), "La proyección debería existir");
        assertEquals("Matrix", proyeccion.get().titulo(), "El título debe coincidir");
        assertEquals(1L, proyeccion.get().version(), "La versión 0 debe normalizarse a 1");
    }

    @Test
    @DisplayName("version igual a actual se ignora por idempotencia")
    void versionIgualAActual_seIgnoraPorIdempotencia() {
        // Setup: Preparar el escenario
        var repo = new InMemoryPeliculaProyeccionRepository();
        repo.guardar(new PeliculaProyeccion("1", "Actual", new java.math.BigDecimal("1000.0"), true, 2L));
        var handler = new MovieEventHandler(repo);
        var payload = new MovieEventPayload(1L, "NoDebeAplicar", 900.0, true, 2L);
        var event = new MovieEventEnvelope("evt-2", "MovieUpdated.v1", java.time.Instant.now(), payload);

        // Ejercitación: Ejecutar la acción a probar
        handler.handle(event);

        // Verificación: Verificar el resultado esperado
        var proyeccion = repo.buscarPorMovieId("1").get();
        assertEquals("Actual", proyeccion.titulo(), "No debería sobrescribir una versión ya aplicada");
        assertEquals(new java.math.BigDecimal("1000.0"), proyeccion.precioActual(), "El precio no debería cambiar");
        assertEquals(2L, proyeccion.version(), "La versión debería actualizarse");
    }

    @Test
    @DisplayName("version menor a actual se ignora por idempotencia")
    void versionMenorAActual_seIgnoraPorIdempotencia() {
        // Setup: Preparar el escenario
        var repo = new InMemoryPeliculaProyeccionRepository();
        repo.guardar(new PeliculaProyeccion("1", "Actual", new java.math.BigDecimal("1000.0"), true, 2L));
        var handler = new MovieEventHandler(repo);
        var payload = new MovieEventPayload(1L, "Vieja", 900.0, true, 1L);
        var event = new MovieEventEnvelope("evt-3", "MovieUpdated.v1", java.time.Instant.now(), payload);

        // Ejercitación: Ejecutar la acción a probar
        handler.handle(event);

        // Verificación: Verificar el resultado esperado
        var proyeccion = repo.buscarPorMovieId("1").get();
        assertEquals("Actual", proyeccion.titulo(), "No debería degradar el título");
        assertEquals(new java.math.BigDecimal("1000.0"), proyeccion.precioActual(), "No debería degradar el precio");
        assertEquals(2L, proyeccion.version(), "La versión no debería cambiar");
    }

    @Test
    @DisplayName("version siguiente se aplica correctamente")
    void versionSiguiente_seAplicaCorrectamente() {
        // Setup: Preparar el escenario
        var repo = new InMemoryPeliculaProyeccionRepository();
        repo.guardar(new PeliculaProyeccion("1", "Activa", new java.math.BigDecimal("1000.0"), true, 2L));
        var handler = new MovieEventHandler(repo);
        var payload = new MovieEventPayload(1L, "Actualizada", 1200.0, false, 3L);
        var event = new MovieEventEnvelope("evt-4", "MovieUpdated.v1", java.time.Instant.now(), payload);

        // Ejercitación: Ejecutar la acción a probar
        handler.handle(event);

        // Verificación: Verificar el resultado esperado
        var proyeccion = repo.buscarPorMovieId("1").get();
        assertEquals("Actualizada", proyeccion.titulo(), "La actualización debe aplicarse");
        assertEquals(new java.math.BigDecimal("1200.0"), proyeccion.precioActual(), "El precio debe actualizarse");
        assertFalse(proyeccion.activa(), "La proyección debería quedar inactiva");
        assertEquals(3L, proyeccion.version(), "La versión debería actualizarse");
    }

    @Test
    @DisplayName("version con gap lanza excepcion controlada")
    void versionConGap_lanzaExcepcionControlada() {
        // Setup: Preparar el escenario
        var repo = new InMemoryPeliculaProyeccionRepository();
        repo.guardar(new PeliculaProyeccion("1", "Activa", new java.math.BigDecimal("1000.0"), true, 2L));
        var handler = new MovieEventHandler(repo);
        var payload = new MovieEventPayload(1L, "NoAplicar", 1500.0, true, 5L);
        var event = new MovieEventEnvelope("evt-5", "MovieUpdated.v1", java.time.Instant.now(), payload);

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(MovieEventVersionGapException.class, () -> handler.handle(event));

        // Verificación: Verificar el resultado esperado
        assertTrue(ex.getMessage().contains("movieId=1"), "El mensaje debe incluir el movieId para diagnóstico");
    }

    private static class InMemoryPeliculaProyeccionRepository implements PeliculaProyeccionRepository {
        private final Map<String, PeliculaProyeccion> data = new ConcurrentHashMap<>();

        @Override
        public Optional<PeliculaProyeccion> buscarPorMovieId(String movieId) {
            return Optional.ofNullable(data.get(movieId));
        }

        @Override
        public void guardar(PeliculaProyeccion proyeccion) {
            data.put(proyeccion.movieId(), proyeccion);
        }

        @Override
        public List<PeliculaProyeccion> buscarTodas() {
            return List.copyOf(data.values());
        }
    }
}
