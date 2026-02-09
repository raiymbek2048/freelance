package kg.freelance.repository;

import kg.freelance.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByExecutorIdAndIsVisibleTrueOrderByCreatedAtDesc(Long executorId, Pageable pageable);

    Optional<Review> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.executor.id = :executorId AND r.isVisible = true")
    Double calculateAverageRating(@Param("executorId") Long executorId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.executor.id = :executorId AND r.isVisible = true")
    long countByExecutorId(@Param("executorId") Long executorId);

    Page<Review> findByIsModeratedFalseOrderByCreatedAtAsc(Pageable pageable);

    // Analytics queries
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.isVisible = true")
    Double calculateOverallAverageRating();

    long countByIsModeratedFalse();
}
