package unrn.event.movie;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import unrn.model.PeliculaProyeccion;
import unrn.repository.PeliculaProyeccionRepository;

@Component
public class MovieEventHandler {

    static final String ERROR_EVENTO_NULO = "El evento no puede ser nulo";
    static final String ERROR_PAYLOAD_NULO = "El payload no puede ser nulo";

    private final PeliculaProyeccionRepository repository;

    public MovieEventHandler(PeliculaProyeccionRepository repository) {
        this.repository = repository;
    }

    public void handle(MovieEventEnvelope envelope) {
        assertEventoNoNulo(envelope);
        assertPayloadNoNulo(envelope.payload());

        var payload = envelope.payload();
        var movieId = String.valueOf(payload.movieId());

        var existente = repository.buscarPorMovieId(movieId);
        if (existente.isPresent() && existente.get().version() >= payload.version()) {
            return;
        }

        var proyeccion = new PeliculaProyeccion(
                movieId,
                payload.title(),
                BigDecimal.valueOf(payload.price()),
                payload.active(),
                payload.version());

        repository.guardar(proyeccion);
    }

    private void assertEventoNoNulo(MovieEventEnvelope envelope) {
        if (envelope == null) {
            throw new RuntimeException(ERROR_EVENTO_NULO);
        }
    }

    private void assertPayloadNoNulo(MovieEventPayload payload) {
        if (payload == null) {
            throw new RuntimeException(ERROR_PAYLOAD_NULO);
        }
    }
}
