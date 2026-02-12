package unrn.repository;

import java.util.Optional;
import unrn.model.PeliculaProyeccion;

public interface PeliculaProyeccionRepository {

    Optional<PeliculaProyeccion> buscarPorMovieId(String movieId);

    void guardar(PeliculaProyeccion proyeccion);
}
