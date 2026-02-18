package unrn.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unrn.dto.ApiErrorResponse;
import unrn.service.ClienteNoAutenticadoException;

@RestControllerAdvice
public class CompraExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CompraExceptionHandler.class);

    private static final String ERROR_CARRITO_VACIO = "No se puede confirmar una compra con carrito vacío";
    private static final String ERROR_FECHAS_DESCUENTO_INCOMPLETAS = "Para aplicar descuento debe informar vigenteDesde y vigenteHasta";
    private static final String ERROR_COMPRA_NO_ENCONTRADA = "No se encontró la compra para el cliente autenticado";

    @ExceptionHandler(ClienteNoAutenticadoException.class)
    public ResponseEntity<ApiErrorResponse> handleClienteNoAutenticado(ClienteNoAutenticadoException ex) {
        ApiErrorResponse body = new ApiErrorResponse("CLIENTE_NO_AUTENTICADO", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntime(RuntimeException ex) {
        log.error("Error en flujo de compra: {}", ex.getMessage(), ex);
        if (ERROR_CARRITO_VACIO.equals(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse("CARRITO_VACIO", ex.getMessage()));
        }

        if (ERROR_FECHAS_DESCUENTO_INCOMPLETAS.equals(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse("DESCUENTO_INVALIDO", ex.getMessage()));
        }

        if (ERROR_COMPRA_NO_ENCONTRADA.equals(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiErrorResponse("COMPRA_NO_ENCONTRADA", ex.getMessage()));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("COMPRA_ERROR", ex.getMessage()));
    }
}