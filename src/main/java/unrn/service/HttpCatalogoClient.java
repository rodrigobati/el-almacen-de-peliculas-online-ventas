package unrn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String catalogoBaseUrl;
    private final int pageSize;

    public HttpCatalogoClient(
            @Value("${ventas.catalogo.base-url}") String catalogoBaseUrl,
            @Value("${ventas.catalogo.page-size:200}") int pageSize) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
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
        try {
            String query = "page=" + paginaActual
                    + "&size=" + pageSize
                    + "&sort=" + encode("id")
                    + "&asc=true";

            String base = catalogoBaseUrl.endsWith("/")
                    ? catalogoBaseUrl.substring(0, catalogoBaseUrl.length() - 1)
                    : catalogoBaseUrl;

            URI uri = URI.create(base + "/peliculas?" + query);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), CatalogoPageResponse.class);
        } catch (Exception ex) {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CatalogoPageResponse(List<CatalogoMovieResponse> items, long total, int totalPages, int page,
            int size) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CatalogoMovieResponse(Long id, String titulo, double precio, Boolean activa, Long version) {
    }
}
