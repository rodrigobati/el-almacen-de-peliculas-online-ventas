package unrn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ProjectionBootstrapServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("almacen_ventas")
            .withUsername("almacen")
            .withPassword("almacen");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final AtomicInteger RESPONSE_DELAY_MS = new AtomicInteger(0);
    private static final List<CatalogoMovieFixture> CATALOGO_MOVIES = new ArrayList<>();
    private static final HttpServer CATALOGO_SERVER = createCatalogoServer();

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driverClassName", mysql::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.flyway.schemas", () -> "almacen_ventas");
        registry.add("spring.flyway.default-schema", () -> "almacen_ventas");
        registry.add("ventas.catalogo.base-url", () -> "http://localhost:" + CATALOGO_SERVER.getAddress().getPort());
        registry.add("ventas.catalogo.page-size", () -> 2);
        registry.add("ventas.bootstrap.internal-token", () -> "test-bootstrap-token");
    }

    @Autowired
    private ProjectionBootstrapService projectionBootstrapService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        // Setup: Preparar el escenario
        jdbcTemplate.update("DELETE FROM pelicula_proyeccion");
        jdbcTemplate.update(
                "UPDATE projection_bootstrap_lock SET locked = 0, locked_at = NULL, owner_id = NULL WHERE lock_name = 'pelicula_projection_rebuild'");
        synchronized (CATALOGO_MOVIES) {
            CATALOGO_MOVIES.clear();
        }
        RESPONSE_DELAY_MS.set(0);
    }

    @AfterAll
    static void afterAll() {
        CATALOGO_SERVER.stop(0);
    }

    @Test
    @DisplayName("rebuildProjection catalogoConPeliculas insertaYActualizaCorrectamente")
    void rebuildProjection_catalogoConPeliculas_insertaYActualizaCorrectamente() {
        // Setup: Preparar el escenario
        jdbcTemplate.update(
                "INSERT INTO pelicula_proyeccion (movie_id, titulo, precio_actual, activa, version) VALUES (?, ?, ?, ?, ?)",
                "1", "Titulo Viejo", new BigDecimal("100.00"), true, 1L);
        jdbcTemplate.update(
                "INSERT INTO pelicula_proyeccion (movie_id, titulo, precio_actual, activa, version) VALUES (?, ?, ?, ?, ?)",
                "999", "Pelicula Huerfana", new BigDecimal("500.00"), true, 3L);

        cargarCatalogo(List.of(
                new CatalogoMovieFixture(1L, "Titulo Nuevo", 110.00, true, 2L),
                new CatalogoMovieFixture(2L, "Pelicula Nueva", 80.00, false, 1L)));

        // Ejercitación: Ejecutar la acción a probar
        ProjectionBootstrapResult result = projectionBootstrapService.rebuildProjection();

        // Verificación: Verificar el resultado esperado
        assertEquals(2, result.fetched(), "Debe procesar todas las películas traídas desde catálogo");
        assertEquals(1, result.inserted(), "Debe insertar la película nueva");
        assertEquals(1, result.updated(), "Debe actualizar la película existente cambiada");
        assertEquals(1, result.deactivated(), "Debe desactivar la película huérfana no presente en catálogo");

        Integer countActivaHuerfana = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pelicula_proyeccion WHERE movie_id = '999' AND activa = 0", Integer.class);
        assertEquals(1, countActivaHuerfana, "La película huérfana debe quedar inactiva en la proyección");
    }

    @Test
    @DisplayName("rebuildProjection ejecucionRepetida idempotente")
    void rebuildProjection_ejecucionRepetida_idempotente() {
        // Setup: Preparar el escenario
        cargarCatalogo(List.of(
                new CatalogoMovieFixture(1L, "Blade Runner", 9999.99, true, 1L),
                new CatalogoMovieFixture(2L, "Matrix", 8000.00, false, 1L)));

        // Ejercitación: Ejecutar la acción a probar
        ProjectionBootstrapResult firstRun = projectionBootstrapService.rebuildProjection();
        ProjectionBootstrapResult secondRun = projectionBootstrapService.rebuildProjection();

        // Verificación: Verificar el resultado esperado
        assertEquals(2, firstRun.inserted(), "La primera ejecución debe insertar ambas películas");
        assertEquals(0, secondRun.inserted(), "La segunda ejecución no debe insertar filas nuevas");
        assertEquals(0, secondRun.updated(), "La segunda ejecución no debe actualizar filas sin cambios");
        assertEquals(0, secondRun.deactivated(), "La segunda ejecución no debe desactivar filas adicionales");

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pelicula_proyeccion", Integer.class);
        assertEquals(2, total, "La proyección debe mantener exactamente dos filas");
    }

    @Test
    @DisplayName("rebuildProjection concurrente bloqueaSegundaEjecucion")
    void rebuildProjection_concurrente_bloqueaSegundaEjecucion() throws Exception {
        // Setup: Preparar el escenario
        cargarCatalogo(List.of(new CatalogoMovieFixture(10L, "Dune", 6500.00, true, 1L)));
        RESPONSE_DELAY_MS.set(700);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<Object> first = executorService.submit(() -> ejecutarConBarrera(startLatch));
        Future<Object> second = executorService.submit(() -> ejecutarConBarrera(startLatch));

        // Ejercitación: Ejecutar la acción a probar
        startLatch.countDown();

        Object r1 = first.get(5, TimeUnit.SECONDS);
        Object r2 = second.get(5, TimeUnit.SECONDS);

        executorService.shutdownNow();

        // Verificación: Verificar el resultado esperado
        int erroresConLock = 0;
        int exitos = 0;

        for (Object result : List.of(r1, r2)) {
            if (result instanceof ProjectionBootstrapResult) {
                exitos++;
            }
            if (result instanceof RuntimeException ex
                    && ProjectionBootstrapService.ERROR_BOOTSTRAP_EN_EJECUCION.equals(ex.getMessage())) {
                erroresConLock++;
            }
        }

        assertEquals(1, exitos, "Solo una ejecución debe completar el rebuild");
        assertEquals(1, erroresConLock, "La segunda ejecución concurrente debe bloquearse");
    }

    private Object ejecutarConBarrera(CountDownLatch latch) {
        try {
            latch.await();
            return projectionBootstrapService.rebuildProjection();
        } catch (Exception ex) {
            return ex;
        }
    }

    private void cargarCatalogo(List<CatalogoMovieFixture> fixtures) {
        synchronized (CATALOGO_MOVIES) {
            CATALOGO_MOVIES.clear();
            CATALOGO_MOVIES.addAll(fixtures);
        }
    }

    private static HttpServer createCatalogoServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/peliculas", ProjectionBootstrapServiceIntegrationTest::handlePeliculas);
            server.start();
            return server;
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo iniciar servidor HTTP de catálogo para tests", ex);
        }
    }

    private static void handlePeliculas(HttpExchange exchange) throws IOException {
        try {
            if (RESPONSE_DELAY_MS.get() > 0) {
                Thread.sleep(RESPONSE_DELAY_MS.get());
            }

            Map<String, String> queryParams = parseQuery(exchange.getRequestURI());
            int page = Integer.parseInt(queryParams.getOrDefault("page", "0"));
            int size = Integer.parseInt(queryParams.getOrDefault("size", "200"));

            List<CatalogoMovieFixture> snapshot;
            synchronized (CATALOGO_MOVIES) {
                snapshot = List.copyOf(CATALOGO_MOVIES);
            }

            int total = snapshot.size();
            int safeSize = size <= 0 ? 1 : size;
            int from = page * safeSize;
            int to = Math.min(from + safeSize, total);

            List<CatalogoMovieFixture> items = from >= total ? List.of() : snapshot.subList(from, to);
            int totalPages = total == 0 ? 1 : (int) Math.ceil((double) total / safeSize);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("items", items);
            response.put("total", total);
            response.put("totalPages", totalPages);
            response.put("page", page);
            response.put("size", safeSize);

            byte[] body = OBJECT_MAPPER.writeValueAsBytes(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            byte[] body = "{\"error\":\"interrupted\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        }
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> params = new HashMap<>();
        String rawQuery = uri.getQuery();

        if (rawQuery == null || rawQuery.isBlank()) {
            return params;
        }

        for (String pair : rawQuery.split("&")) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }

        return params;
    }

    private record CatalogoMovieFixture(Long id, String titulo, double precio, boolean activa, Long version) {
    }
}
