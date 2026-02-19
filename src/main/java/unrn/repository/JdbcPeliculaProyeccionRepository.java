package unrn.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import unrn.model.PeliculaProyeccion;

@Repository
public class JdbcPeliculaProyeccionRepository implements PeliculaProyeccionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPeliculaProyeccionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PeliculaProyeccion> buscarPorMovieId(String movieId) {
        var sql = "SELECT movie_id, titulo, precio_actual, activa, version FROM pelicula_proyeccion WHERE movie_id = ?";
        var results = jdbcTemplate.query(sql, new PeliculaProyeccionRowMapper(), movieId);
        return results.stream().findFirst();
    }

    @Override
    public void guardar(PeliculaProyeccion proyeccion) {
        var updated = jdbcTemplate.update(
                "UPDATE pelicula_proyeccion SET titulo = ?, precio_actual = ?, activa = ?, version = ? WHERE movie_id = ?",
                proyeccion.titulo(),
                proyeccion.precioActual(),
                proyeccion.activa(),
                proyeccion.version(),
                proyeccion.movieId());

        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO pelicula_proyeccion (movie_id, titulo, precio_actual, activa, version) VALUES (?, ?, ?, ?, ?)",
                    proyeccion.movieId(),
                    proyeccion.titulo(),
                    proyeccion.precioActual(),
                    proyeccion.activa(),
                    proyeccion.version());
        }
    }

    @Override
    public List<PeliculaProyeccion> buscarTodas() {
        var sql = "SELECT movie_id, titulo, precio_actual, activa, version FROM pelicula_proyeccion";
        return jdbcTemplate.query(sql, new PeliculaProyeccionRowMapper());
    }

    private static class PeliculaProyeccionRowMapper implements RowMapper<PeliculaProyeccion> {
        @Override
        public PeliculaProyeccion mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PeliculaProyeccion(
                    rs.getString("movie_id"),
                    rs.getString("titulo"),
                    rs.getBigDecimal("precio_actual"),
                    rs.getBoolean("activa"),
                    rs.getLong("version"));
        }
    }
}
