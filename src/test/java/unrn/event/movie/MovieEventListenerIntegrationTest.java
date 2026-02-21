package unrn.event.movie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import unrn.model.PeliculaProyeccion;
import unrn.repository.PeliculaProyeccionRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovieEventListenerIntegrationTest {

    @Test
    @DisplayName("Consumo de eventos con version invalida no bloquea y permite seguir procesando")
    void consumoEventos_conVersionInvalidaNoBloquea_permiteSeguirProcesando() {
        // Setup: Preparar el escenario
        var repo = new InMemoryPeliculaProyeccionRepository();
        var handler = new MovieEventHandler(repo);
        var listener = new MovieEventListener(handler);

        var createdV0 = new MovieEventEnvelope(
                "evt-1",
                "MovieCreated.v1",
                Instant.now(),
                new MovieEventPayload(1L, "Matrix", 1000.0, true, 0L));

        var gapV3 = new MovieEventEnvelope(
                "evt-2",
                "MovieUpdated.v1",
                Instant.now(),
                new MovieEventPayload(1L, "Matrix Reloaded", 1100.0, true, 3L));

        var updateV2 = new MovieEventEnvelope(
                "evt-3",
                "MovieUpdated.v1",
                Instant.now(),
                new MovieEventPayload(1L, "Matrix Reloaded", 1100.0, true, 2L));

        // Ejercitación: Ejecutar la acción a probar
        assertDoesNotThrow(() -> listener.onMovieEvent(createdV0));
        assertDoesNotThrow(() -> listener.onMovieEvent(gapV3));
        assertDoesNotThrow(() -> listener.onMovieEvent(updateV2));

        // Verificación: Verificar el resultado esperado
        var proyeccion = repo.buscarPorMovieId("1");
        assertTrue(proyeccion.isPresent(), "La proyección debe existir luego de consumir eventos");
        assertEquals(2L, proyeccion.get().version(), "Debe aplicar el evento válido posterior al rechazado");
        assertEquals("Matrix Reloaded", proyeccion.get().titulo(), "Debe quedar la versión más reciente aplicable");
        assertEquals(new BigDecimal("1100.0"), proyeccion.get().precioActual(), "El precio debe quedar actualizado");
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
