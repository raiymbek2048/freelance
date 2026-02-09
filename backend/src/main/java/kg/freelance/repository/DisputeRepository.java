package kg.freelance.repository;

import kg.freelance.entity.Dispute;
import kg.freelance.entity.enums.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    Optional<Dispute> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    Page<Dispute> findByStatusOrderByCreatedAtDesc(DisputeStatus status, Pageable pageable);

    Page<Dispute> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE d.status IN ('OPEN', 'UNDER_REVIEW') ORDER BY d.createdAt ASC")
    Page<Dispute> findActiveDisputes(Pageable pageable);

    long countByStatus(DisputeStatus status);
}
