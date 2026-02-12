package unrn.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unrn.dto.ActualizarStockRequest;
import unrn.dto.StockDTO;
import unrn.service.StockService;

@RestController
@RequestMapping("/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/{peliculaId}")
    public ResponseEntity<StockDTO> obtenerStock(@PathVariable String peliculaId) {
        int stock = stockService.stockDisponible(peliculaId);
        return ResponseEntity.ok(new StockDTO(peliculaId, stock));
    }

    @PutMapping("/{peliculaId}")
    public ResponseEntity<StockDTO> actualizarStock(
            @PathVariable String peliculaId,
            @RequestBody ActualizarStockRequest request) {
        stockService.actualizarStock(peliculaId, request.stockDisponible());
        int stock = stockService.stockDisponible(peliculaId);
        return ResponseEntity.ok(new StockDTO(peliculaId, stock));
    }
}
