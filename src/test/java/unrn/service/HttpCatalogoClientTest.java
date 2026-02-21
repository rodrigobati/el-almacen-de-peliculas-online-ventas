package unrn.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpCatalogoClientTest {

    private static final AtomicReference<StubResponse> RESPONSE = new AtomicReference<>();
    private static final HttpServer SERVER = crearServidor();

    @BeforeEach
    void beforeEach() {
        // Setup: Preparar el escenario
        RESPONSE.set(new StubResponse(200, "application/json",
                "{\"items\":[{\"id\":1,\"titulo\":\"Matrix\",\"precio\":1000.0,\"activa\":true,\"version\":1}],\"total\":1,\"totalPages\":1,\"page\":0,\"size\":200}"));
    }

    @AfterAll
    static void afterAll() {
        SERVER.stop(0);
    }

    @Test
    @DisplayName("ObtenerTodasLasPeliculas con respuesta paginada válida devuelve snapshots")
    void obtenerTodasLasPeliculas_respuestaPaginadaValida_devuelveSnapshots() {
        // Setup: Preparar el escenario
        var client = new HttpCatalogoClient("http://localhost:" + SERVER.getAddress().getPort(), 200);

        // Ejercitación: Ejecutar la acción a probar
        var snapshots = client.obtenerTodasLasPeliculas();

        // Verificación: Verificar el resultado esperado
        assertEquals(1, snapshots.size(), "Debe devolver una película del catálogo");
        assertEquals(1L, snapshots.get(0).movieId(), "Debe mapear el id de película");
        assertEquals(1L, snapshots.get(0).version(), "Debe conservar la versión válida del catálogo");
    }

    @Test
    @DisplayName("ObtenerTodasLasPeliculas con estado no exitoso lanza excepción descriptiva")
    void obtenerTodasLasPeliculas_estadoNoExitoso_lanzaExcepcionDescriptiva() {
        // Setup: Preparar el escenario
        RESPONSE.set(new StubResponse(503, "application/json", "{\"error\":\"downstream unavailable\"}"));
        var client = new HttpCatalogoClient("http://localhost:" + SERVER.getAddress().getPort(), 200);

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, client::obtenerTodasLasPeliculas);

        // Verificación: Verificar el resultado esperado
        assertTrue(ex.getMessage().contains(HttpCatalogoClient.ERROR_STATUS_HTTP_CATALOGO),
                "Debe indicar que el status HTTP es inválido");
        assertTrue(ex.getMessage().contains("status=503"), "Debe incluir el status HTTP recibido");
    }

    @Test
    @DisplayName("ObtenerTodasLasPeliculas con respuesta sin items lanza excepción descriptiva")
    void obtenerTodasLasPeliculas_respuestaSinItems_lanzaExcepcionDescriptiva() {
        // Setup: Preparar el escenario
        RESPONSE.set(
                new StubResponse(200, "application/json", "{\"total\":1,\"totalPages\":1,\"page\":0,\"size\":200}"));
        var client = new HttpCatalogoClient("http://localhost:" + SERVER.getAddress().getPort(), 200);

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, client::obtenerTodasLasPeliculas);

        // Verificación: Verificar el resultado esperado
        assertEquals(HttpCatalogoClient.ERROR_RESPUESTA_CATALOGO_SIN_ITEMS, ex.getMessage(),
                "Debe informar que la respuesta no trae el arreglo de items");
    }

    @Test
    @DisplayName("ObtenerTodasLasPeliculas con JSON malformado lanza excepción descriptiva")
    void obtenerTodasLasPeliculas_jsonMalformado_lanzaExcepcionDescriptiva() {
        // Setup: Preparar el escenario
        RESPONSE.set(new StubResponse(200, "application/json", "{items:[}"));
        var client = new HttpCatalogoClient("http://localhost:" + SERVER.getAddress().getPort(), 200);

        // Ejercitación: Ejecutar la acción a probar
        var ex = assertThrows(RuntimeException.class, client::obtenerTodasLasPeliculas);

        // Verificación: Verificar el resultado esperado
        assertTrue(ex.getMessage().contains(HttpCatalogoClient.ERROR_JSON_CATALOGO_INVALIDO),
                "Debe reportar JSON inválido de catálogo");
        assertTrue(ex.getMessage().contains("bodySnippet="), "Debe incluir snippet del body para diagnóstico");
    }

    private static HttpServer crearServidor() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/peliculas", HttpCatalogoClientTest::responder);
            server.start();
            return server;
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo iniciar servidor stub de catálogo", ex);
        }
    }

    private static void responder(HttpExchange exchange) throws IOException {
        StubResponse stubResponse = RESPONSE.get();
        byte[] bytes = stubResponse.body().getBytes(StandardCharsets.UTF_8);

        if (stubResponse.contentType() != null) {
            exchange.getResponseHeaders().set("Content-Type", stubResponse.contentType());
        }

        exchange.sendResponseHeaders(stubResponse.status(), bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private record StubResponse(int status, String contentType, String body) {
    }
}
