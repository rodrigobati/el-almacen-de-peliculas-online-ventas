package unrn.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unrn.dto.AgregarPeliculaRequest;
import unrn.dto.CarritoDTO;
import unrn.service.CarritoService;

@RestController
@RequestMapping("/clientes/{clienteId}/carrito")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @GetMapping
    public ResponseEntity<CarritoDTO> verCarrito(@PathVariable String clienteId) {
        CarritoDTO carrito = carritoService.verCarrito(clienteId);
        return ResponseEntity.ok(carrito);
    }

    @PostMapping("/items")
    public ResponseEntity<CarritoDTO> agregarPelicula(
            @PathVariable String clienteId,
            @RequestBody AgregarPeliculaRequest request) {
        CarritoDTO carrito = carritoService.agregarPelicula(clienteId, request);
        return ResponseEntity.ok(carrito);
    }

    @DeleteMapping("/items/{peliculaId}")
    public ResponseEntity<CarritoDTO> eliminarPelicula(
            @PathVariable String clienteId,
            @PathVariable String peliculaId) {
        CarritoDTO carrito = carritoService.eliminarPelicula(clienteId, peliculaId);
        return ResponseEntity.ok(carrito);
    }
}
