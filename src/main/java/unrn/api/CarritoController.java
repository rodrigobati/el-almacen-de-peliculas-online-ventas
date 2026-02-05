package unrn.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unrn.dto.AgregarPeliculaRequest;
import unrn.dto.CarritoDTO;
import unrn.service.CarritoService;

@RestController
@RequestMapping("/carrito")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @GetMapping
    public ResponseEntity<CarritoDTO> verCarrito() {
        CarritoDTO carrito = carritoService.verCarrito();
        return ResponseEntity.ok(carrito);
    }

    @PostMapping("/items")
    public ResponseEntity<CarritoDTO> agregarPelicula(@RequestBody AgregarPeliculaRequest request) {
        CarritoDTO carrito = carritoService.agregarPelicula(request);
        return ResponseEntity.ok(carrito);
    }

    @DeleteMapping("/items/{peliculaId}")
    public ResponseEntity<CarritoDTO> eliminarPelicula(@PathVariable String peliculaId) {
        CarritoDTO carrito = carritoService.eliminarPelicula(peliculaId);
        return ResponseEntity.ok(carrito);
    }
}
