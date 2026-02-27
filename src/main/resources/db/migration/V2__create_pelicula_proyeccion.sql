CREATE TABLE IF NOT EXISTS pelicula_proyeccion (
    movie_id VARCHAR(64) NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    precio_actual DECIMAL(12,2) NOT NULL,
    activa BOOLEAN NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT pk_pelicula_proyeccion PRIMARY KEY (movie_id)
);