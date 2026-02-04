package kg.freelance.repository;

import kg.freelance.entity.ExecutorProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ExecutorProfileRepository extends JpaRepository<ExecutorProfile, Long> {

    @Query("""
            SELECT e FROM ExecutorProfile e
            JOIN e.user u
            WHERE u.active = true
            AND u.hideFromExecutorList = false
            AND (:categoryId IS NULL OR :categoryId IN (SELECT c.id FROM e.categories c))
            AND (:minRating IS NULL OR e.rating >= :minRating)
            AND (:availableOnly = false OR e.availableForWork = true)
            AND (:search IS NULL OR u.fullName LIKE %:search% OR e.specialization LIKE %:search%)
            """)
    Page<ExecutorProfile> findExecutors(
            @Param("categoryId") Long categoryId,
            @Param("minRating") BigDecimal minRating,
            @Param("availableOnly") boolean availableOnly,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT e FROM ExecutorProfile e
            JOIN e.categories c
            WHERE c.id = :categoryId
            AND e.user.active = true
            AND e.user.hideFromExecutorList = false
            ORDER BY e.rating DESC
            """)
    List<ExecutorProfile> findTopExecutorsByCategory(@Param("categoryId") Long categoryId, Pageable pageable);
}
