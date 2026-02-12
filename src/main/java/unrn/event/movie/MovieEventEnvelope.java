package unrn.event.movie;

import java.time.Instant;

public record MovieEventEnvelope(
        String eventId,
        String eventType,
        Instant occurredAt,
        MovieEventPayload payload) {

    static final String TYPE_CREATED = "MovieCreated.v1";
    static final String TYPE_UPDATED = "MovieUpdated.v1";
    static final String TYPE_RETIRED = "MovieRetired.v1";
}
