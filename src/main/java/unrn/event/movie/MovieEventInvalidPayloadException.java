package unrn.event.movie;

class MovieEventInvalidPayloadException extends MovieEventNoRetryException {

    MovieEventInvalidPayloadException(String message) {
        super(message);
    }
}
