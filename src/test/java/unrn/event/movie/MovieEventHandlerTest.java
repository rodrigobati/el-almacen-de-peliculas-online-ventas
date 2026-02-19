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
    @DisplayName("aplicarMovieCreated creaProyeccion")
    void aplicarMovieCreated_creaProyeccion() {
        // Setup: Preparar el escenario
        var repo = new InMemoryPeliculaProyeccionRepository();
        var handler = new MovieEventHandler(repo);
        var payload = new MovieEventPayload(1L, "Matrix", 1000.0, true, 1L);
        var event = new MovieEventEnvelope("evt-1", "MovieCreated.v1", java.time.Instant.now(), payload);

        // Ejercitación: Ejecutar la acción a probar
        handler.handle(event);

        // Verificación: Verificar el resultado esperado
        var proyeccion = repo.buscarPorMovieId("1");
        assertTrue(proyeccion.isPresent(), "La proyección debería existir");
        assertEquals("Matrix", proyeccion.get().titulo(), "El título debe coincidir");
    }

    @Test
    @DisplayName("aplicarMovieUpdated actualizaProyeccion")
    void aplicarMovieUpdated_actualizaProyeccion() {
        // Setup: Preparar el escenario
        var repo = new InMemoryPeliculaProyeccionRepository();
        repo.guardar(new PeliculaProyeccion("1", "Vieja", new java.math.BigDecimal("900.0"), true, 1L));
        var handler = new MovieEventHandler(repo);
        var payload = new MovieEventPayload(1L, "Nueva", 1000.0, true, 2L);
        var event = new MovieEventEnvelope("evt-2", "MovieUpdated.v1", java.time.Instant.now(), payload);

        // Ejercitación: Ejecutar la acción a probar
        handler.handle(event);

        // Verificación: Verificar el resultado esperado
        var proyeccion = repo.buscarPorMovieId("1").get();
        assertEquals("Nueva", proyeccion.titulo(), "El título debería actualizarse");
        assertEquals(new java.math.BigDecimal("1000.0"), proyeccion.precioActual(), "El precio debería actualizarse");
        assertEquals(2L, proyeccion.version(), "La versión debería actualizarse");
    }

    @Test
    @DisplayName("aplicarMovieUpdatedVersionVieja seIgnora")
    void aplicarMovieUpdatedVersionVieja_seIgnora() {
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
    @DisplayName("aplicarMovieRetired desactiva")
    void aplicarMovieRetired_desactiva() {
        // Setup: Preparar el escenario
        var repo = new InMemoryPeliculaProyeccionRepository();
        repo.guardar(new PeliculaProyeccion("1", "Activa", new java.math.BigDecimal("1000.0"), true, 1L));
        var handler = new MovieEventHandler(repo);
        var payload = new MovieEventPayload(1L, "Activa", 1000.0, false, 2L);
        var event = new MovieEventEnvelope("evt-4", "MovieRetired.v1", java.time.Instant.now(), payload);

        // Ejercitación: Ejecutar la acción a probar
        handler.handle(event);

        // Verificación: Verificar el resultado esperado
        var proyeccion = repo.buscarPorMovieId("1").get();
        assertFalse(proyeccion.activa(), "La proyección debería quedar inactiva");
        assertEquals(2L, proyeccion.version(), "La versión debería actualizarse");
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
