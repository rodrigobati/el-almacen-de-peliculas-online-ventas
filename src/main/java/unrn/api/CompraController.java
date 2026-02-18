package unrn.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unrn.dto.CarritoCompraResponse;
import unrn.dto.CompraDetalleResponse;
import unrn.dto.CompraResumenResponse;
import unrn.dto.ConfirmarCompraRequest;
import unrn.dto.ConfirmarCompraResponse;
import unrn.service.ConfirmarCompraService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CompraController {

    private final ConfirmarCompraService confirmarCompraService;

    public CompraController(ConfirmarCompraService confirmarCompraService) {
        this.confirmarCompraService = confirmarCompraService;
    }

    @GetMapping("/carrito")
    public ResponseEntity<CarritoCompraResponse> verCarrito() {
        return ResponseEntity.ok(confirmarCompraService.verCarrito());
    }

    @PostMapping("/carrito/confirmar")
    public ResponseEntity<ConfirmarCompraResponse> confirmarCompra(
            @RequestBody(required = false) ConfirmarCompraRequest request) {
        ConfirmarCompraResponse response = confirmarCompraService.confirmarCompra(request);
        return ResponseEntity.created(URI.create("/api/compras/" + response.compraId())).body(response);
    }

    @GetMapping("/compras")
    public ResponseEntity<List<CompraResumenResponse>> historialCompras() {
        return ResponseEntity.ok(confirmarCompraService.historialCompras());
    }

    @GetMapping("/compras/{id}")
    public ResponseEntity<CompraDetalleResponse> detalleCompra(@PathVariable("id") Long compraId) {
        return ResponseEntity.ok(confirmarCompraService.detalleCompra(compraId));
    }
}