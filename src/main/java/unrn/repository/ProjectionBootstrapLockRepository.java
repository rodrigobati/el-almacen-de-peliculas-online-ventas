package unrn.repository;

public interface ProjectionBootstrapLockRepository {

    boolean intentarAdquirir(String lockName, String ownerId);

    void liberar(String lockName, String ownerId);
}
