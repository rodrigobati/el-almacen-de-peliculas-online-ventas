package unrn.event.movie;

class MovieEventNoRetryException extends RuntimeException {

    MovieEventNoRetryException(String message) {
        super(message);
    }
}
