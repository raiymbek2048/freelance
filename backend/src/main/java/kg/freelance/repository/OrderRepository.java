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
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            SELECT o FROM Order o
            WHERE o.isPublic = true
            AND o.status = kg.freelance.entity.enums.OrderStatus.NEW
            AND (:categoryId IS NULL OR o.category.id = :categoryId)
            AND (:budgetMin IS NULL OR o.budgetMax >= :budgetMin)
            AND (:budgetMax IS NULL OR o.budgetMin <= :budgetMax)
            AND (:search IS NULL OR o.title LIKE %:search% OR o.description LIKE %:search%)
            ORDER BY o.createdAt DESC
            """)
    Page<Order> findPublicOrders(
            @Param("categoryId") Long categoryId,
            @Param("budgetMin") BigDecimal budgetMin,
            @Param("budgetMax") BigDecimal budgetMax,
            @Param("search") String search,
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
}
