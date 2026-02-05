package unrn.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import unrn.dto.AgregarPeliculaRequest;
import unrn.dto.CarritoDTO;
import unrn.repository.CarritoRepository;
import unrn.repository.InMemoryCarritoRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CarritoServiceTest {

    private CarritoRepository repository;
    private CarritoService service;
    private static final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        repository = new InMemoryCarritoRepository();
        service = new CarritoService(repository);
        configurarUsuarioAutenticado(TEST_USERNAME);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void configurarUsuarioAutenticado(String username) {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .claim("sub", username)
                .claim("preferred_username", username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        authentication.setAuthenticated(true);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("VerCarrito sin carrito previo devuelve carrito vacío con total cero")
    void verCarrito_sinCarritoPrevio_devuelveCarritoVacioTotalCero() {
        // Setup: Usuario autenticado (configurado en @BeforeEach)

        // Ejercitación: Ver el carrito
        CarritoDTO resultado = service.verCarrito();

        // Verificación: Carrito vacío con total en cero
        assertNotNull(resultado, "El carrito DTO no debería ser nulo");
        assertTrue(resultado.items().isEmpty(), "Los items deberían estar vacíos");
        assertEquals(BigDecimal.ZERO, resultado.total(), "El total debería ser cero");
    }

    @Test
    @DisplayName("AgregarPelicula primera vez agrega item y total correcto")
    void agregarPelicula_primeraVez_agregaItemYTotalCorrecto() {
        // Setup: Usuario autenticado, request válido
        AgregarPeliculaRequest request = new AgregarPeliculaRequest(
                "pelicula-1",
                "Matrix",
                new BigDecimal("10.50"),
                2);

        // Ejercitación: Agregar película
        CarritoDTO resultado = service.agregarPelicula(request);

        // Verificación: Item agregado con total correcto
        assertEquals(1, resultado.items().size(), "Debería haber un item en el carrito");
        assertEquals("pelicula-1", resultado.items().get(0).peliculaId(), "El id de la película debería coincidir");
        assertEquals("Matrix", resultado.items().get(0).titulo(), "El título debería coincidir");
        assertEquals(new BigDecimal("10.50"), resultado.items().get(0).precioUnitario(),
                "El precio unitario debería coincidir");
        assertEquals(2, resultado.items().get(0).cantidad(), "La cantidad debería ser 2");
        assertEquals(new BigDecimal("21.00"), resultado.items().get(0).subtotal(), "El subtotal debería ser 21.00");
        assertEquals(new BigDecimal("21.00"), resultado.total(), "El total debería ser 21.00");
    }

    @Test
    @DisplayName("AgregarPelicula misma película incrementa cantidad y total correcto")
    void agregarPelicula_mismaPelicula_incrementaCantidadYTotalCorrecto() {
        // Setup: Agregar película primera vez
        AgregarPeliculaRequest request1 = new AgregarPeliculaRequest(
                "pelicula-1",
                "Matrix",
                new BigDecimal("10.00"),
                2);
        service.agregarPelicula(request1);

        // Ejercitación: Agregar misma película nuevamente
        AgregarPeliculaRequest request2 = new AgregarPeliculaRequest(
                "pelicula-1",
                "Matrix",
                new BigDecimal("10.00"),
                3);
        CarritoDTO resultado = service.agregarPelicula(request2);

        // Verificación: Cantidad incrementada y total actualizado
        assertEquals(1, resultado.items().size(), "Debería seguir habiendo un solo item");
        assertEquals(5, resultado.items().get(0).cantidad(), "La cantidad debería ser 5 (2 + 3)");
        assertEquals(new BigDecimal("50.00"), resultado.items().get(0).subtotal(), "El subtotal debería ser 50.00");
        assertEquals(new BigDecimal("50.00"), resultado.total(), "El total debería ser 50.00");
    }

    @Test
    @DisplayName("EliminarPelicula existente elimina y actualiza total")
    void eliminarPelicula_existente_eliminaYActualizaTotal() {
        // Setup: Agregar dos películas diferentes
        AgregarPeliculaRequest request1 = new AgregarPeliculaRequest(
                "pelicula-1",
                "Matrix",
                new BigDecimal("10.00"),
                2);
        AgregarPeliculaRequest request2 = new AgregarPeliculaRequest(
                "pelicula-2",
                "Inception",
                new BigDecimal("15.00"),
                1);
        service.agregarPelicula(request1);
        service.agregarPelicula(request2);

        // Ejercitación: Eliminar primera película
        CarritoDTO resultado = service.eliminarPelicula("pelicula-1");

        // Verificación: Solo queda una película y total actualizado
        assertEquals(1, resultado.items().size(), "Debería quedar solo un item");
        assertEquals("pelicula-2", resultado.items().get(0).peliculaId(), "Debería quedar solo la película 2");
        assertEquals(new BigDecimal("15.00"), resultado.total(), "El total debería ser 15.00");
    }

    @Test
    @DisplayName("EliminarPelicula inexistente lanza excepción con mensaje del dominio")
    void eliminarPelicula_inexistente_lanzaExcepcionConMensajeDominio() {
        // Setup: Usuario con carrito vacío

        // Ejercitación y Verificación: Eliminar película inexistente lanza excepción
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.eliminarPelicula("pelicula-inexistente");
        });
        assertNotNull(ex.getMessage(), "El mensaje de error no debería ser nulo");
        assertTrue(ex.getMessage().contains("encontr") || ex.getMessage().contains("item"),
                "El mensaje debería indicar que el item no fue encontrado");
    }

    @Test
    @DisplayName("AgregarPelicula con cantidad inválida lanza excepción")
    void agregarPelicula_conCantidadInvalida_lanzaExcepcion() {
        // Setup: Request con cantidad cero
        AgregarPeliculaRequest request = new AgregarPeliculaRequest(
                "pelicula-1",
                "Matrix",
                new BigDecimal("10.00"),
                0);

        // Ejercitación y Verificación: Agregar con cantidad inválida lanza excepción
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.agregarPelicula(request);
        });
        assertNotNull(ex.getMessage(), "El mensaje de error no debería ser nulo");
        assertTrue(ex.getMessage().contains("cantidad") || ex.getMessage().contains("mayor"),
                "El mensaje debería indicar problema con la cantidad");
    }

    @Test
    @DisplayName("Usuario no autenticado lanza excepción")
    void usuario_noAutenticado_lanzaExcepcion() {
        // Setup: Limpiar el contexto de seguridad
        SecurityContextHolder.clearContext();

        // Ejercitación y Verificación: Sin usuario autenticado lanza excepción
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.verCarrito();
        });
        assertEquals(CarritoService.ERROR_USUARIO_NO_AUTENTICADO, ex.getMessage(),
                "El mensaje de error debería indicar que el usuario no está autenticado");
    }

    @Test
    @DisplayName("AgregarPelicula con request nulo lanza excepción")
    void agregarPelicula_requestNulo_lanzaExcepcion() {
        // Setup: Usuario autenticado con request nulo
        AgregarPeliculaRequest request = null;

        // Ejercitación y Verificación: Request nulo lanza excepción
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.agregarPelicula(request);
        });
        assertEquals(CarritoService.ERROR_REQUEST_NULO, ex.getMessage(),
                "El mensaje de error debería indicar que el request es nulo");
    }
}
