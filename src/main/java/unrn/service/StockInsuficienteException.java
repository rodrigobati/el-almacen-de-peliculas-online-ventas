package unrn.service;

public class StockInsuficienteException extends RuntimeException {

    static final String ERROR_STOCK_INSUFICIENTE = "Stock insuficiente para la película";

    private final String peliculaId;
    private final int availableStock;
    private final int requestedQuantity;

    public StockInsuficienteException(String peliculaId, int availableStock, int requestedQuantity) {
        super(ERROR_STOCK_INSUFICIENTE);
        this.peliculaId = peliculaId;
        this.availableStock = availableStock;
        this.requestedQuantity = requestedQuantity;
    }

    public String peliculaId() {
        return peliculaId;
    }

    public int availableStock() {
        return availableStock;
    }

    public int requestedQuantity() {
        return requestedQuantity;
    }
}
