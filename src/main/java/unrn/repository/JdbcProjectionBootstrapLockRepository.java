package unrn.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProjectionBootstrapLockRepository implements ProjectionBootstrapLockRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProjectionBootstrapLockRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean intentarAdquirir(String lockName, String ownerId) {
        var updated = jdbcTemplate.update(
                "UPDATE projection_bootstrap_lock SET locked = 1, locked_at = CURRENT_TIMESTAMP, owner_id = ? WHERE lock_name = ? AND locked = 0",
                ownerId,
                lockName);
        return updated == 1;
    }

    @Override
    public void liberar(String lockName, String ownerId) {
        jdbcTemplate.update(
                "UPDATE projection_bootstrap_lock SET locked = 0, locked_at = NULL, owner_id = NULL WHERE lock_name = ? AND owner_id = ?",
                lockName,
                ownerId);
    }
}
