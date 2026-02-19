package unrn.dto;

public record ProjectionBootstrapResponse(
        int fetched,
        int inserted,
        int updated,
        int deactivated,
        long durationMs) {
}
