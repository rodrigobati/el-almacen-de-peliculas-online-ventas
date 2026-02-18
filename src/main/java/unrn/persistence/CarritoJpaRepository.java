package unrn.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarritoJpaRepository extends JpaRepository<CarritoEntity, Long> {

    Optional<CarritoEntity> findByClienteId(String clienteId);
}