package unrn.event.movie;

class MovieEventVersionGapException extends MovieEventNoRetryException {

    MovieEventVersionGapException(String baseMessage,
            String movieId,
            long currentVersion,
            long incomingVersion,
            String eventId,
            String eventType) {
        super(baseMessage + " movieId=" + movieId
                + " currentVersion=" + currentVersion
                + " incomingVersion=" + incomingVersion
                + " eventId=" + eventId
                + " eventType=" + eventType);
    }
}
