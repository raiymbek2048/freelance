package kg.freelance.service;

import kg.freelance.dto.request.*;
import kg.freelance.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {

    // Public orders
    PageResponse<OrderListResponse> getPublicOrders(
            Long categoryId,
            BigDecimal budgetMin,
            BigDecimal budgetMax,
            String search,
            Pageable pageable
    );

    OrderDetailResponse getOrderById(Long orderId, Long userId);

    // Client operations
    OrderDetailResponse createOrder(Long clientId, OrderCreateRequest request);

    OrderDetailResponse updateOrder(Long clientId, Long orderId, OrderUpdateRequest request);

    void deleteOrder(Long clientId, Long orderId);

    void selectExecutor(Long clientId, Long orderId, SelectExecutorRequest request);

    void approveWork(Long clientId, Long orderId);

    void requestRevision(Long clientId, Long orderId, String reason);

    void openDispute(Long userId, Long orderId, String reason);

    // Executor operations
    void startWork(Long executorId, Long orderId);

    void submitForReview(Long executorId, Long orderId);

    // My orders
    PageResponse<OrderListResponse> getMyOrdersAsClient(Long clientId, Pageable pageable);

    PageResponse<OrderListResponse> getMyOrdersAsExecutor(Long executorId, Pageable pageable);

    // Responses
    List<OrderResponseDto> getOrderResponses(Long orderId, Long userId);

    OrderResponseDto createResponse(Long executorId, Long orderId, OrderResponseRequest request);

    OrderResponseDto updateResponse(Long executorId, Long responseId, OrderResponseRequest request);

    void deleteResponse(Long executorId, Long responseId);

    PageResponse<OrderResponseDto> getMyResponses(Long executorId, Pageable pageable);
}
