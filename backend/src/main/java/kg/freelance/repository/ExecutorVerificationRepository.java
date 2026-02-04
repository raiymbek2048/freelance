package kg.freelance.repository;

import kg.freelance.entity.ExecutorVerification;
import kg.freelance.entity.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExecutorVerificationRepository extends JpaRepository<ExecutorVerification, Long> {

    Optional<ExecutorVerification> findByUserId(Long userId);

    Page<ExecutorVerification> findByStatusOrderBySubmittedAtDesc(VerificationStatus status, Pageable pageable);

    Page<ExecutorVerification> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(v) FROM ExecutorVerification v WHERE v.status = :status")
    long countByStatus(@Param("status") VerificationStatus status);

    boolean existsByUserId(Long userId);
}
