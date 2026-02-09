package kg.freelance.repository;

import kg.freelance.entity.Order;
import kg.freelance.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query(value = """
            SELECT * FROM orders o
            WHERE o.is_public = true
            AND o.status = 'NEW'
            AND (o.deadline IS NULL OR o.deadline >= CURRENT_DATE)
            AND (CAST(:categoryId AS BIGINT) IS NULL OR o.category_id = :categoryId)
            AND (CAST(:budgetMin AS NUMERIC) IS NULL OR o.budget_max >= :budgetMin)
            AND (CAST(:budgetMax AS NUMERIC) IS NULL OR o.budget_min <= :budgetMax)
            AND (:search IS NULL OR to_tsvector('russian', coalesce(o.title, '')) @@ plainto_tsquery('russian', :search))
            AND (:location IS NULL OR o.location ILIKE '%' || :location || '%')
            """,
            countQuery = """
            SELECT COUNT(*) FROM orders o
            WHERE o.is_public = true
            AND o.status = 'NEW'
            AND (o.deadline IS NULL OR o.deadline >= CURRENT_DATE)
            AND (CAST(:categoryId AS BIGINT) IS NULL OR o.category_id = :categoryId)
            AND (CAST(:budgetMin AS NUMERIC) IS NULL OR o.budget_max >= :budgetMin)
            AND (CAST(:budgetMax AS NUMERIC) IS NULL OR o.budget_min <= :budgetMax)
            AND (:search IS NULL OR to_tsvector('russian', coalesce(o.title, '')) @@ plainto_tsquery('russian', :search))
            AND (:location IS NULL OR o.location ILIKE '%' || :location || '%')
            """,
            nativeQuery = true)
    Page<Order> findPublicOrders(
            @Param("categoryId") Long categoryId,
            @Param("budgetMin") BigDecimal budgetMin,
            @Param("budgetMax") BigDecimal budgetMax,
            @Param("search") String search,
            @Param("location") String location,
            Pageable pageable
    );

    Page<Order> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    Page<Order> findByExecutorIdOrderByCreatedAtDesc(Long executorId, Pageable pageable);

    List<Order> findByClientIdAndStatusInOrderByCreatedAtDesc(Long clientId, List<OrderStatus> statuses);

    List<Order> findByExecutorIdAndStatusInOrderByCreatedAtDesc(Long executorId, List<OrderStatus> statuses);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.executor.id = :executorId AND o.status = :status")
    long countByExecutorIdAndStatus(@Param("executorId") Long executorId, @Param("status") OrderStatus status);

    @Modifying
    @Query("UPDATE Order o SET o.viewCount = o.viewCount + 1 WHERE o.id = :orderId")
    void incrementViewCount(@Param("orderId") Long orderId);

    @Modifying
    @Query("UPDATE Order o SET o.responseCount = o.responseCount + 1 WHERE o.id = :orderId")
    void incrementResponseCount(@Param("orderId") Long orderId);

    // Analytics queries
    long countByStatus(@Param("status") OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.agreedPrice), 0) FROM Order o WHERE o.agreedPrice IS NOT NULL")
    BigDecimal sumAgreedPrice();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.agreedPrice IS NOT NULL")
    long countWithAgreedPrice();

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.completedAt BETWEEN :start AND :end")
    long countCompletedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT o.category.id, o.category.name, COUNT(o) FROM Order o GROUP BY o.category.id, o.category.name ORDER BY COUNT(o) DESC")
    List<Object[]> countOrdersByCategory();
}
