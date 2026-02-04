package kg.freelance.repository;

import kg.freelance.entity.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderResponseRepository extends JpaRepository<OrderResponse, Long> {

    List<OrderResponse> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    Page<OrderResponse> findByExecutorIdOrderByCreatedAtDesc(Long executorId, Pageable pageable);

    Optional<OrderResponse> findByOrderIdAndExecutorId(Long orderId, Long executorId);

    boolean existsByOrderIdAndExecutorId(Long orderId, Long executorId);

    long countByOrderId(Long orderId);
}
