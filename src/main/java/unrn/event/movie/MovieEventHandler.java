package unrn.event.movie;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import unrn.model.PeliculaProyeccion;
import unrn.repository.PeliculaProyeccionRepository;

@Component
public class MovieEventHandler {

    private static final Logger log = LoggerFactory.getLogger(MovieEventHandler.class);

    static final String ERROR_EVENTO_NULO = "El evento no puede ser nulo";
    static final String ERROR_PAYLOAD_NULO = "El payload no puede ser nulo";
    static final String ERROR_MOVIE_ID_NULO = "El movieId del payload no puede ser nulo";
    static final String ERROR_VERSION_GAP = "Evento de película fuera de secuencia";

    private final PeliculaProyeccionRepository repository;

    public MovieEventHandler(PeliculaProyeccionRepository repository) {
        this.repository = repository;
    }

    public void handle(MovieEventEnvelope envelope) {
        assertEventoNoNulo(envelope);
        assertPayloadNoNulo(envelope.payload());

        var payload = envelope.payload();
        assertMovieIdNoNulo(payload.movieId());
        var movieId = String.valueOf(payload.movieId());
        var incomingVersion = payload.version();

        var existente = repository.buscarPorMovieId(movieId);

        if (existente.isEmpty()) {
            long versionPersistida = normalizarVersionInicial(movieId, incomingVersion, envelope);
            guardarProyeccion(payload, movieId, versionPersistida);
            return;
        }

        var currentVersion = existente.get().version();

        if (incomingVersion <= currentVersion) {
            log.info(
                    "evento_movie_ignorado_idempotencia eventId={} eventType={} movieId={} currentVersion={} incomingVersion={}",
                    envelope.eventId(),
                    envelope.eventType(),
                    movieId,
                    currentVersion,
                    incomingVersion);
            return;
        }

        if (incomingVersion > currentVersion + 1) {
            throw new MovieEventVersionGapException(
                    ERROR_VERSION_GAP,
                    movieId,
                    currentVersion,
                    incomingVersion,
                    envelope.eventId(),
                    envelope.eventType());
        }

        guardarProyeccion(payload, movieId, incomingVersion);
    }

    private long normalizarVersionInicial(String movieId, long incomingVersion, MovieEventEnvelope envelope) {
        if (incomingVersion == 0L) {
            log.warn(
                    "evento_movie_version_normalizada eventId={} eventType={} movieId={} incomingVersion={} normalizedVersion={}",
                    envelope.eventId(),
                    envelope.eventType(),
                    movieId,
                    incomingVersion,
                    1L);
            return 1L;
        }

        if (incomingVersion < 0L) {
            throw new MovieEventInvalidPayloadException(ERROR_VERSION_GAP + ": versión negativa para movieId=" + movieId
                    + " incomingVersion=" + incomingVersion);
        }

        return incomingVersion;
    }

    private void guardarProyeccion(MovieEventPayload payload, String movieId, long version) {
        var proyeccion = new PeliculaProyeccion(
                movieId,
                payload.title(),
                BigDecimal.valueOf(payload.price()),
                payload.active(),
                version);

        repository.guardar(proyeccion);
    }

    private void assertEventoNoNulo(MovieEventEnvelope envelope) {
        if (envelope == null) {
            throw new MovieEventInvalidPayloadException(ERROR_EVENTO_NULO);
        }
    }

    private void assertPayloadNoNulo(MovieEventPayload payload) {
        if (payload == null) {
            throw new MovieEventInvalidPayloadException(ERROR_PAYLOAD_NULO);
        }
    }

    private void assertMovieIdNoNulo(Long movieId) {
        if (movieId == null) {
            throw new MovieEventInvalidPayloadException(ERROR_MOVIE_ID_NULO);
        }
    }
}
