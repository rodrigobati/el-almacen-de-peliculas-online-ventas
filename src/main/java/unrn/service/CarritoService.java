package unrn.service;

import org.springframework.stereotype.Service;
import unrn.dto.AgregarPeliculaRequest;
import unrn.dto.CarritoDTO;
import unrn.dto.PeliculaEnCarritoDTO;
import unrn.model.Carrito;
import unrn.model.PeliculaEnCarrito;
import unrn.repository.CarritoRepository;

import java.util.List;

@Service
public class CarritoService {

    static final String ERROR_CLIENTE_ID_NULO = "El id del cliente no puede ser nulo";
    static final String ERROR_CLIENTE_ID_VACIO = "El id del cliente no puede estar vacío";
    static final String ERROR_REQUEST_NULO = "El request no puede ser nulo";

    private final CarritoRepository carritoRepository;

    public CarritoService(CarritoRepository carritoRepository) {
        this.carritoRepository = carritoRepository;
    }

    public CarritoDTO verCarrito(String clienteId) {
        assertClienteIdNoNulo(clienteId);
        assertClienteIdNoVacio(clienteId);

        Carrito carrito = carritoRepository.obtenerDe(clienteId);
        return mapearADTO(carrito);
    }

    public CarritoDTO agregarPelicula(String clienteId, AgregarPeliculaRequest request) {
        assertClienteIdNoNulo(clienteId);
        assertClienteIdNoVacio(clienteId);
        assertRequestNoNulo(request);

        Carrito carrito = carritoRepository.obtenerDe(clienteId);
        carrito.agregarPelicula(
                request.peliculaId(),
                request.titulo(),
                request.precioUnitario(),
                request.cantidad());
        carritoRepository.guardar(clienteId, carrito);

        return mapearADTO(carrito);
    }

    public CarritoDTO eliminarPelicula(String clienteId, String peliculaId) {
        assertClienteIdNoNulo(clienteId);
        assertClienteIdNoVacio(clienteId);

        Carrito carrito = carritoRepository.obtenerDe(clienteId);
        carrito.eliminarPelicula(peliculaId);
        carritoRepository.guardar(clienteId, carrito);

        return mapearADTO(carrito);
    }

    private void assertClienteIdNoNulo(String clienteId) {
        if (clienteId == null) {
            throw new RuntimeException(ERROR_CLIENTE_ID_NULO);
        }
    }

    private void assertClienteIdNoVacio(String clienteId) {
        if (clienteId.trim().isEmpty()) {
            throw new RuntimeException(ERROR_CLIENTE_ID_VACIO);
        }
    }

    private void assertRequestNoNulo(AgregarPeliculaRequest request) {
        if (request == null) {
            throw new RuntimeException(ERROR_REQUEST_NULO);
        }
    }

    private CarritoDTO mapearADTO(Carrito carrito) {
        List<PeliculaEnCarritoDTO> itemsDTO = carrito.items().stream()
                .map(this::mapearItemADTO)
                .toList();

        return new CarritoDTO(itemsDTO, carrito.total());
    }

    private PeliculaEnCarritoDTO mapearItemADTO(PeliculaEnCarrito item) {
        return new PeliculaEnCarritoDTO(
                item.peliculaId(),
                item.titulo(),
                item.precioUnitario(),
                item.cantidad(),
                item.subtotal());
    }
}
