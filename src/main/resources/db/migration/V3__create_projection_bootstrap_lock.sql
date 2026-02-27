CREATE TABLE IF NOT EXISTS projection_bootstrap_lock (
    lock_name VARCHAR(64) NOT NULL,
    locked BOOLEAN NOT NULL,
    locked_at TIMESTAMP NULL,
    owner_id VARCHAR(128) NULL,
    CONSTRAINT pk_projection_bootstrap_lock PRIMARY KEY (lock_name)
);

INSERT INTO projection_bootstrap_lock (lock_name, locked, locked_at, owner_id)
SELECT 'pelicula_projection_rebuild', 0, NULL, NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM projection_bootstrap_lock
    WHERE lock_name = 'pelicula_projection_rebuild'
);
