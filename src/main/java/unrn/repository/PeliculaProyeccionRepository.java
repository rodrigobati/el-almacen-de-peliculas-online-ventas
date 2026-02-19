package unrn.repository;

import java.util.List;
import java.util.Optional;
import unrn.model.PeliculaProyeccion;

public interface PeliculaProyeccionRepository {

    Optional<PeliculaProyeccion> buscarPorMovieId(String movieId);

    void guardar(PeliculaProyeccion proyeccion);

    List<PeliculaProyeccion> buscarTodas();
}
