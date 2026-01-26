package unrn.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import unrn.dto.AgregarPeliculaRequest;
import unrn.dto.CarritoDTO;
import unrn.model.Carrito;
import unrn.repository.CarritoRepository;
import unrn.repository.InMemoryCarritoRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CarritoServiceTest {

    private CarritoRepository repository;
    private CarritoService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCarritoRepository();
        service = new CarritoService(repository);
    }

    @Test
    @DisplayName("VerCarrito sin carrito previo devuelve carrito vacío con total cero")
    void verCarrito_sinCarritoPrevio_devuelveCarritoVacioTotalCero() {
        // Setup: Cliente sin carrito previo
        String clienteId = "cliente-123";

        // Ejercitación: Ver el carrito
        CarritoDTO resultado = service.verCarrito(clienteId);

        // Verificación: Carrito vacío con total en cero
        assertNotNull(resultado, "El carrito DTO no debería ser nulo");
        assertTrue(resultado.items().isEmpty(), "Los items deberían estar vacíos");
        assertEquals(BigDecimal.ZERO, resultado.total(), "El total debería ser cero");
    }

    @Test
    @DisplayName("AgregarPelicula primera vez agrega item y total correcto")
    void agregarPelicula_primeraVez_agregaItemYTotalCorrecto() {
        // Setup: Cliente sin carrito previo
        String clienteId = "cliente-123";
        AgregarPeliculaRequest request = new AgregarPeliculaRequest(
                "pelicula-1",
                "Matrix",
                new BigDecimal("10.50"),
                2);

        // Ejercitación: Agregar película
        CarritoDTO resultado = service.agregarPelicula(clienteId, request);

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
        String clienteId = "cliente-123";
        AgregarPeliculaRequest request1 = new AgregarPeliculaRequest(
                "pelicula-1",
                "Matrix",
                new BigDecimal("10.00"),
                2);
        service.agregarPelicula(clienteId, request1);

        // Ejercitación: Agregar misma película nuevamente
        AgregarPeliculaRequest request2 = new AgregarPeliculaRequest(
                "pelicula-1",
                "Matrix",
                new BigDecimal("10.00"),
                3);
        CarritoDTO resultado = service.agregarPelicula(clienteId, request2);

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
        String clienteId = "cliente-123";
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
        service.agregarPelicula(clienteId, request1);
        service.agregarPelicula(clienteId, request2);

        // Ejercitación: Eliminar primera película
        CarritoDTO resultado = service.eliminarPelicula(clienteId, "pelicula-1");

        // Verificación: Solo queda una película y total actualizado
        assertEquals(1, resultado.items().size(), "Debería quedar solo un item");
        assertEquals("pelicula-2", resultado.items().get(0).peliculaId(), "Debería quedar solo la película 2");
        assertEquals(new BigDecimal("15.00"), resultado.total(), "El total debería ser 15.00");
    }

    @Test
    @DisplayName("EliminarPelicula inexistente lanza excepción con mensaje del dominio")
    void eliminarPelicula_inexistente_lanzaExcepcionConMensajeDominio() {
        // Setup: Cliente con carrito vacío
        String clienteId = "cliente-123";

        // Ejercitación y Verificación: Eliminar película inexistente lanza excepción
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.eliminarPelicula(clienteId, "pelicula-inexistente");
        });
        assertNotNull(ex.getMessage(), "El mensaje de error no debería ser nulo");
        assertTrue(ex.getMessage().contains("encontr") || ex.getMessage().contains("item"),
                "El mensaje debería indicar que el item no fue encontrado");
    }

    @Test
    @DisplayName("AgregarPelicula con cantidad inválida lanza excepción")
    void agregarPelicula_conCantidadInvalida_lanzaExcepcion() {
        // Setup: Request con cantidad cero
        String clienteId = "cliente-123";
        AgregarPeliculaRequest request = new AgregarPeliculaRequest(
                "pelicula-1",
                "Matrix",
                new BigDecimal("10.00"),
                0);

        // Ejercitación y Verificación: Agregar con cantidad inválida lanza excepción
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.agregarPelicula(clienteId, request);
        });
        assertNotNull(ex.getMessage(), "El mensaje de error no debería ser nulo");
        assertTrue(ex.getMessage().contains("cantidad") || ex.getMessage().contains("mayor"),
                "El mensaje debería indicar problema con la cantidad");
    }

    @Test
    @DisplayName("ClienteId vacío lanza excepción")
    void clienteId_vacio_lanzaExcepcion() {
        // Setup: Cliente con id vacío
        String clienteId = "   ";

        // Ejercitación y Verificación: ClienteId vacío lanza excepción
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.verCarrito(clienteId);
        });
        assertEquals(CarritoService.ERROR_CLIENTE_ID_VACIO, ex.getMessage(),
                "El mensaje de error debería indicar que el id está vacío");
    }

    @Test
    @DisplayName("ClienteId nulo lanza excepción")
    void clienteId_nulo_lanzaExcepcion() {
        // Setup: Cliente con id nulo
        String clienteId = null;

        // Ejercitación y Verificación: ClienteId nulo lanza excepción
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.verCarrito(clienteId);
        });
        assertEquals(CarritoService.ERROR_CLIENTE_ID_NULO, ex.getMessage(),
                "El mensaje de error debería indicar que el id es nulo");
    }

    @Test
    @DisplayName("AgregarPelicula con request nulo lanza excepción")
    void agregarPelicula_requestNulo_lanzaExcepcion() {
        // Setup: Cliente válido con request nulo
        String clienteId = "cliente-123";
        AgregarPeliculaRequest request = null;

        // Ejercitación y Verificación: Request nulo lanza excepción
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.agregarPelicula(clienteId, request);
        });
        assertEquals(CarritoService.ERROR_REQUEST_NULO, ex.getMessage(),
                "El mensaje de error debería indicar que el request es nulo");
    }
}
