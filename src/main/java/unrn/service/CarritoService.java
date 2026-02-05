package unrn.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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

    static final String ERROR_USUARIO_NO_AUTENTICADO = "Usuario no autenticado";
    static final String ERROR_REQUEST_NULO = "El request no puede ser nulo";

    private final CarritoRepository carritoRepository;

    public CarritoService(CarritoRepository carritoRepository) {
        this.carritoRepository = carritoRepository;
    }

    public CarritoDTO verCarrito() {
        String clienteId = obtenerClienteIdAutenticado();
        Carrito carrito = carritoRepository.obtenerDe(clienteId);
        return mapearADTO(carrito);
    }

    public CarritoDTO agregarPelicula(AgregarPeliculaRequest request) {
        assertRequestNoNulo(request);
        String clienteId = obtenerClienteIdAutenticado();

        Carrito carrito = carritoRepository.obtenerDe(clienteId);
        carrito.agregarPelicula(
                request.peliculaId(),
                request.titulo(),
                request.precioUnitario(),
                request.cantidad());
        carritoRepository.guardar(clienteId, carrito);

        return mapearADTO(carrito);
    }

    public CarritoDTO eliminarPelicula(String peliculaId) {
        String clienteId = obtenerClienteIdAutenticado();
        Carrito carrito = carritoRepository.obtenerDe(clienteId);
        carrito.eliminarPelicula(peliculaId);
        carritoRepository.guardar(clienteId, carrito);

        return mapearADTO(carrito);
    }

    private String obtenerClienteIdAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException(ERROR_USUARIO_NO_AUTENTICADO);
        }

        // Si es JwtAuthenticationToken, obtener el claim preferred_username
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String preferredUsername = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
        }

        // Fallback: usar getName()
        String username = auth.getName();
        if (username == null || username.isBlank()) {
            throw new RuntimeException(ERROR_USUARIO_NO_AUTENTICADO);
        }

        return username;
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
