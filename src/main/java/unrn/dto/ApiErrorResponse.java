package unrn.dto;

public record ApiErrorResponse(
        String code,
        String message) {
}