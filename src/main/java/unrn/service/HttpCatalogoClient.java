package unrn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class HttpCatalogoClient implements CatalogoClient {

    static final String ERROR_RESPUESTA_CATALOGO_NULA = "La respuesta del catálogo no puede ser nula";
    static final String ERROR_PELICULA_CATALOGO_NULA = "La película recibida del catálogo no puede ser nula";
    static final String ERROR_ID_PELICULA_CATALOGO_NULO = "El id de película recibido del catálogo no puede ser nulo";
    static final String ERROR_CONSUMIENDO_CATALOGO = "No se pudo consumir catálogo para bootstrap de proyección";
    static final String ERROR_STATUS_HTTP_CATALOGO = "Catálogo respondió estado HTTP inválido";
    static final String ERROR_CONTENT_TYPE_CATALOGO = "Catálogo respondió content-type no JSON";
    static final String ERROR_RESPUESTA_CATALOGO_SIN_ITEMS = "La respuesta del catálogo no incluye items";
    static final String ERROR_BODY_CATALOGO_VACIO = "La respuesta del catálogo no puede ser vacía";
    static final String ERROR_JSON_CATALOGO_INVALIDO = "La respuesta JSON de catálogo es inválida";

    private static final int MAX_BODY_SNIPPET = 220;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String catalogoBaseUrl;
    private final int pageSize;

    @Autowired
    public HttpCatalogoClient(
            @Value("${ventas.catalogo.base-url}") String catalogoBaseUrl,
            @Value("${ventas.catalogo.page-size:200}") int pageSize) {
        this(HttpClient.newHttpClient(), new ObjectMapper(), catalogoBaseUrl, pageSize);
    }

    HttpCatalogoClient(HttpClient httpClient,
            ObjectMapper objectMapper,
            String catalogoBaseUrl,
            int pageSize) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.catalogoBaseUrl = catalogoBaseUrl;
        this.pageSize = pageSize;
    }

    @Override
    public List<CatalogoPeliculaSnapshot> obtenerTodasLasPeliculas() {
        int paginaActual = 0;
        int totalPaginas = 1;
        Map<Long, CatalogoPeliculaSnapshot> snapshotsPorId = new LinkedHashMap<>();

        while (paginaActual < totalPaginas) {
            var respuesta = obtenerPagina(paginaActual);
            assertRespuestaNoNula(respuesta);
            assertItemsNoNulos(respuesta.items());

            for (var pelicula : respuesta.items()) {
                assertPeliculaNoNula(pelicula);
                assertMovieIdNoNulo(pelicula.id());
                var snapshot = new CatalogoPeliculaSnapshot(
                        pelicula.id(),
                        pelicula.titulo(),
                        BigDecimal.valueOf(pelicula.precio()),
                        pelicula.activa() == null ? true : pelicula.activa(),
                        pelicula.version() == null || pelicula.version() <= 0 ? 1L : pelicula.version());
                snapshotsPorId.put(snapshot.movieId(), snapshot);
            }

            totalPaginas = respuesta.totalPages() <= 0 ? 1 : respuesta.totalPages();
            paginaActual++;
        }

        return new ArrayList<>(snapshotsPorId.values());
    }

    private CatalogoPageResponse obtenerPagina(int paginaActual) {
        URI uri = null;
        try {
            String query = "page=" + paginaActual
                    + "&size=" + pageSize
                    + "&sort=" + encode("id")
                    + "&asc=true";

            String base = catalogoBaseUrl.endsWith("/")
                    ? catalogoBaseUrl.substring(0, catalogoBaseUrl.length() - 1)
                    : catalogoBaseUrl;

            uri = URI.create(base + "/peliculas?" + query);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertStatusHttpValido(uri, response);
            assertContentTypeJson(uri, response);

            String body = response.body();
            assertBodyNoVacio(uri, response.statusCode(), body);

            try {
                return objectMapper.readValue(body, CatalogoPageResponse.class);
            } catch (Exception ex) {
                throw construirErrorCatalogo(ERROR_JSON_CATALOGO_INVALIDO, uri, response.statusCode(), body, ex);
            }
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }

            throw new RuntimeException(ERROR_CONSUMIENDO_CATALOGO, ex);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void assertRespuestaNoNula(CatalogoPageResponse respuesta) {
        if (respuesta == null) {
            throw new RuntimeException(ERROR_RESPUESTA_CATALOGO_NULA);
        }
    }

    private void assertItemsNoNulos(List<CatalogoMovieResponse> items) {
        if (items == null) {
            throw new RuntimeException(ERROR_RESPUESTA_CATALOGO_SIN_ITEMS);
        }
    }

    private void assertPeliculaNoNula(CatalogoMovieResponse pelicula) {
        if (pelicula == null) {
            throw new RuntimeException(ERROR_PELICULA_CATALOGO_NULA);
        }
    }

    private void assertMovieIdNoNulo(Long movieId) {
        if (movieId == null) {
            throw new RuntimeException(ERROR_ID_PELICULA_CATALOGO_NULO);
        }
    }

    private void assertStatusHttpValido(URI uri, HttpResponse<String> response) {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw construirErrorCatalogo(ERROR_STATUS_HTTP_CATALOGO, uri, status, response.body(), null);
        }
    }

    private void assertContentTypeJson(URI uri, HttpResponse<String> response) {
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        if (!contentType.toLowerCase().contains("application/json")) {
            throw construirErrorCatalogo(
                    ERROR_CONTENT_TYPE_CATALOGO + ": " + contentType,
                    uri,
                    response.statusCode(),
                    response.body(),
                    null);
        }
    }

    private void assertBodyNoVacio(URI uri, int status, String body) {
        if (body == null || body.isBlank()) {
            throw construirErrorCatalogo(ERROR_BODY_CATALOGO_VACIO, uri, status, body, null);
        }
    }

    private RuntimeException construirErrorCatalogo(String mensajeBase,
            URI uri,
            int status,
            String body,
            Exception cause) {
        String mensaje = mensajeBase
                + " url=" + uri
                + " status=" + status
                + " bodySnippet=" + snippet(body);

        if (cause == null) {
            return new RuntimeException(mensaje);
        }

        return new RuntimeException(mensaje, cause);
    }

    private String snippet(String body) {
        if (body == null || body.isBlank()) {
            return "<empty>";
        }

        String normalized = body.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_BODY_SNIPPET) {
            return normalized;
        }

        return normalized.substring(0, MAX_BODY_SNIPPET) + "...";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CatalogoPageResponse(List<CatalogoMovieResponse> items, long total, int totalPages, int page,
            int size) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CatalogoMovieResponse(Long id, String titulo, double precio, Boolean activa, Long version) {
    }
}
