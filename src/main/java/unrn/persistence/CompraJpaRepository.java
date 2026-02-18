package unrn.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompraJpaRepository extends JpaRepository<CompraEntity, Long> {

    List<CompraEntity> findByClienteIdOrderByFechaHoraDesc(String clienteId);

    Optional<CompraEntity> findByIdAndClienteId(Long id, String clienteId);
}