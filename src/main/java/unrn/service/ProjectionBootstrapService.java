package unrn.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import unrn.model.PeliculaProyeccion;
import unrn.repository.PeliculaProyeccionRepository;
import unrn.repository.ProjectionBootstrapLockRepository;

@Service
public class ProjectionBootstrapService {

    static final String ERROR_BOOTSTRAP_EN_EJECUCION = "Ya existe un rebuild de proyección en ejecución";
    static final String LOCK_NAME = "pelicula_projection_rebuild";

    private static final Logger LOG = LoggerFactory.getLogger(ProjectionBootstrapService.class);

    private final CatalogoClient catalogoClient;
    private final PeliculaProyeccionRepository proyeccionRepository;
    private final ProjectionBootstrapLockRepository lockRepository;
    private final TransactionTemplate transactionTemplate;

    public ProjectionBootstrapService(CatalogoClient catalogoClient,
            PeliculaProyeccionRepository proyeccionRepository,
            ProjectionBootstrapLockRepository lockRepository,
            TransactionTemplate transactionTemplate) {
        this.catalogoClient = catalogoClient;
        this.proyeccionRepository = proyeccionRepository;
        this.lockRepository = lockRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public ProjectionBootstrapResult rebuildProjection() {
        long inicio = System.currentTimeMillis();
        String ownerId = UUID.randomUUID().toString();

        if (!lockRepository.intentarAdquirir(LOCK_NAME, ownerId)) {
            throw new RuntimeException(ERROR_BOOTSTRAP_EN_EJECUCION);
        }

        try {
            List<CatalogoPeliculaSnapshot> peliculasCatalogo = catalogoClient.obtenerTodasLasPeliculas();
            var acumulado = new ProjectionCounters();
            var idsCatalogo = new HashSet<String>();

            for (var peliculaCatalogo : peliculasCatalogo) {
                idsCatalogo.add(String.valueOf(peliculaCatalogo.movieId()));
                var delta = processMovie(peliculaCatalogo);
                acumulado.sumar(delta);
            }

            acumulado.deactivated += deactivateMissingMovies(idsCatalogo);

            long durationMs = System.currentTimeMillis() - inicio;
            LOG.info(
                    "projection-bootstrap-summary fetched={}, inserted={}, updated={}, deactivated={}, durationMs={}",
                    peliculasCatalogo.size(),
                    acumulado.inserted,
                    acumulado.updated,
                    acumulado.deactivated,
                    durationMs);

            return new ProjectionBootstrapResult(
                    peliculasCatalogo.size(),
                    acumulado.inserted,
                    acumulado.updated,
                    acumulado.deactivated,
                    durationMs);
        } finally {
            lockRepository.liberar(LOCK_NAME, ownerId);
        }
    }

    private ProjectionCounters processMovie(CatalogoPeliculaSnapshot peliculaCatalogo) {
        return transactionTemplate.execute(status -> {
            var counters = new ProjectionCounters();
            var movieId = String.valueOf(peliculaCatalogo.movieId());
            var existente = proyeccionRepository.buscarPorMovieId(movieId);

            if (existente.isEmpty()) {
                proyeccionRepository.guardar(new PeliculaProyeccion(
                        movieId,
                        peliculaCatalogo.titulo(),
                        peliculaCatalogo.precio(),
                        peliculaCatalogo.activa(),
                        peliculaCatalogo.version()));
                counters.inserted = 1;
                return counters;
            }

            var actual = existente.get();
            long versionDestino = Math.max(actual.version(), peliculaCatalogo.version());
            var destino = new PeliculaProyeccion(
                    movieId,
                    peliculaCatalogo.titulo(),
                    peliculaCatalogo.precio(),
                    peliculaCatalogo.activa(),
                    versionDestino);

            if (hasChanges(actual, destino)) {
                proyeccionRepository.guardar(destino);
                if (actual.activa() && !destino.activa()) {
                    counters.deactivated = 1;
                } else {
                    counters.updated = 1;
                }
            }

            return counters;
        });
    }

    private int deactivateMissingMovies(Set<String> idsCatalogo) {
        var peliculasEnProyeccion = proyeccionRepository.buscarTodas();
        int deactivated = 0;

        for (var existente : peliculasEnProyeccion) {
            if (!idsCatalogo.contains(existente.movieId()) && existente.activa()) {
                var inactiva = new PeliculaProyeccion(
                        existente.movieId(),
                        existente.titulo(),
                        existente.precioActual(),
                        false,
                        existente.version() + 1);

                transactionTemplate.executeWithoutResult(status -> proyeccionRepository.guardar(inactiva));
                deactivated++;
            }
        }

        return deactivated;
    }

    private boolean hasChanges(PeliculaProyeccion actual, PeliculaProyeccion destino) {
        if (!actual.titulo().equals(destino.titulo())) {
            return true;
        }

        if (actual.precioActual().compareTo(destino.precioActual()) != 0) {
            return true;
        }

        if (actual.activa() != destino.activa()) {
            return true;
        }

        return actual.version() != destino.version();
    }

    private static class ProjectionCounters {
        private int inserted;
        private int updated;
        private int deactivated;

        private void sumar(ProjectionCounters other) {
            this.inserted += other.inserted;
            this.updated += other.updated;
            this.deactivated += other.deactivated;
        }
    }
}
