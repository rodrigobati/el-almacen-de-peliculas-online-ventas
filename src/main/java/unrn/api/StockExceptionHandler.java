package unrn.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import unrn.dto.StockErrorResponse;
import unrn.service.StockInsuficienteException;

@RestControllerAdvice
public class StockExceptionHandler {

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<StockErrorResponse> handleStockInsuficiente(StockInsuficienteException ex) {
        StockErrorResponse response = new StockErrorResponse(
                "STOCK_INSUFFICIENT",
                ex.getMessage(),
                ex.peliculaId(),
                ex.availableStock(),
                ex.requestedQuantity());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}
