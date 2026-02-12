package unrn.dto;

public record StockErrorResponse(
        String error,
        String message,
        String peliculaId,
        int availableStock,
        int requestedQuantity) {
}
