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
import unrn.model.Carrito;
import unrn.model.PeliculaProyeccion;
import unrn.repository.CarritoRepository;
import unrn.repository.InMemoryCarritoRepository;
import unrn.repository.InMemoryStockRepository;
import unrn.repository.PeliculaProyeccionRepository;
import unrn.repository.StockRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class CarritoServiceTest {

    private CarritoRepository repository;
    private CarritoService service;
    private StockRepository stockRepository;
    private StockService stockService;
    private PeliculaProyeccionRepository proyeccionRepository;
    private static final String TEST_USERNAME = "testuser";
    private static final String ERROR_ITEM_NO_ENCONTRADO = "No se encontró un item con el id de película especificado";
    private static final String ERROR_CANTIDAD_INVALIDA = "La cantidad debe ser mayor a cero";

    @BeforeEach
    void setUp() {
        repository = new InMemoryCarritoRepository();
        stockRepository = new InMemoryStockRepository();
        stockService = new StockService(stockRepository);
        proyeccionRepository = new InMemoryPeliculaProyeccionRepository();
        service = new CarritoService(repository, stockService, proyeccionRepository);
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
        registrarProyeccion("pelicula-1", "Matrix", new BigDecimal("10.50"));
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
    @DisplayName("AddToCart peliculaExistenteEnProyeccion ok")
    void addToCart_peliculaExistenteEnProyeccion_ok() {
        // Setup: Preparar el escenario
        registrarProyeccion("pelicula-77", "Interstellar", new BigDecimal("1500.00"));
        AgregarPeliculaRequest request = new AgregarPeliculaRequest(
                "pelicula-77",
                "Interstellar",
                new BigDecimal("1500.00"),
                1);

        // Ejercitación: Ejecutar la acción a probar
        CarritoDTO resultado = service.agregarPelicula(request);

        // Verificación: Verificar el resultado esperado
        assertEquals(1, resultado.items().size(), "Debe existir un único item en el carrito");
        assertEquals("pelicula-77", resultado.items().get(0).peliculaId(), "El id proyectado debe coincidir");
        assertEquals(new BigDecimal("1500.00"), resultado.total(), "El total debe calcularse con el precio proyectado");
    }

    @Test
    @DisplayName("AgregarPelicula misma película incrementa cantidad y total correcto")
    void agregarPelicula_mismaPelicula_incrementaCantidadYTotalCorrecto() {
        // Setup: Agregar película primera vez
        registrarProyeccion("pelicula-1", "Matrix", new BigDecimal("10.00"));
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
        registrarProyeccion("pelicula-1", "Matrix", new BigDecimal("10.00"));
        registrarProyeccion("pelicula-2", "Inception", new BigDecimal("15.00"));
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
        assertEquals(ERROR_ITEM_NO_ENCONTRADO, ex.getMessage(),
                "El mensaje debería indicar que el item no fue encontrado");
    }

    @Test
    @DisplayName("AgregarPelicula con cantidad inválida lanza excepción")
    void agregarPelicula_conCantidadInvalida_lanzaExcepcion() {
        // Setup: Request con cantidad cero
        registrarProyeccion("pelicula-1", "Matrix", new BigDecimal("10.00"));
        AgregarPeliculaRequest request = new AgregarPeliculaRequest(
                "pelicula-1",
                "Matrix",
                new BigDecimal("10.00"),
                0);

        // Ejercitación y Verificación: Agregar con cantidad inválida lanza excepción
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.agregarPelicula(request);
        });
        assertEquals(ERROR_CANTIDAD_INVALIDA, ex.getMessage(),
                "El mensaje debería indicar problema con la cantidad");
    }

    @Test
    @DisplayName("AgregarPelicula supera stock disponible lanza excepción")
    void agregarPelicula_superaStockDisponible_lanzaExcepcion() {
        // Setup: Stock disponible bajo
        registrarProyeccion("pelicula-1", "Matrix", new BigDecimal("10.00"));
        stockService.actualizarStock("pelicula-1", 1);
        AgregarPeliculaRequest request = new AgregarPeliculaRequest(
                "pelicula-1",
                "Matrix",
                new BigDecimal("10.00"),
                2);

        // Ejercitación
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.agregarPelicula(request);
        });

        // Verificación
        assertEquals(StockInsuficienteException.ERROR_STOCK_INSUFICIENTE, ex.getMessage(),
                "Debe lanzar error de stock insuficiente");
    }

    @Test
    @DisplayName("DecrementarPelicula con cantidad mayor a uno reduce cantidad")
    void decrementarPelicula_cantidadMayorAUno_reduceCantidad() {
        // Setup: Agregar película con cantidad 2
        registrarProyeccion("pelicula-1", "Matrix", new BigDecimal("10.00"));
        AgregarPeliculaRequest request = new AgregarPeliculaRequest(
                "pelicula-1",
                "Matrix",
                new BigDecimal("10.00"),
                2);
        service.agregarPelicula(request);

        // Ejercitación: Decrementar
        CarritoDTO resultado = service.decrementarPelicula("pelicula-1");

        // Verificación: Cantidad 1
        assertEquals(1, resultado.items().size(), "Debe seguir habiendo un item");
        assertEquals(1, resultado.items().get(0).cantidad(), "La cantidad debe ser 1");
        assertEquals(new BigDecimal("10.00"), resultado.total(), "El total debe ser 10.00");
    }

    @Test
    @DisplayName("DecrementarPelicula con cantidad uno elimina item")
    void decrementarPelicula_cantidadUno_eliminaItem() {
        // Setup: Agregar película con cantidad 1
        registrarProyeccion("pelicula-1", "Matrix", new BigDecimal("10.00"));
        AgregarPeliculaRequest request = new AgregarPeliculaRequest(
                "pelicula-1",
                "Matrix",
                new BigDecimal("10.00"),
                1);
        service.agregarPelicula(request);

        // Ejercitación
        CarritoDTO resultado = service.decrementarPelicula("pelicula-1");

        // Verificación
        assertTrue(resultado.items().isEmpty(), "El carrito debe quedar vacío");
        assertEquals(BigDecimal.ZERO, resultado.total(), "El total debe ser cero");
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

    private void registrarProyeccion(String peliculaId, String titulo, BigDecimal precio) {
        proyeccionRepository.guardar(new PeliculaProyeccion(peliculaId, titulo, precio, true, 1L));
    }

    private static class InMemoryPeliculaProyeccionRepository implements PeliculaProyeccionRepository {
        private final Map<String, PeliculaProyeccion> data = new ConcurrentHashMap<>();

        @Override
        public Optional<PeliculaProyeccion> buscarPorMovieId(String movieId) {
            return Optional.ofNullable(data.get(movieId));
        }

        @Override
        public void guardar(PeliculaProyeccion proyeccion) {
            data.put(proyeccion.movieId(), proyeccion);
        }

        @Override
        public List<PeliculaProyeccion> buscarTodas() {
            return List.copyOf(data.values());
        }
    }
}
