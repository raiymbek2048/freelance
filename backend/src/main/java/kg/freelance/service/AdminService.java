package kg.freelance.service;

import kg.freelance.dto.request.CategoryCreateRequest;
import kg.freelance.dto.response.*;
import kg.freelance.dto.response.AnalyticsResponse;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.entity.enums.UserRole;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    // Users
    PageResponse<AdminUserResponse> getAllUsers(String search, Boolean active, UserRole role, Pageable pageable);

    AdminUserResponse getUserById(Long userId);

    void blockUser(Long userId);

    void unblockUser(Long userId);

    void changeUserRole(Long userId, UserRole role);

    // Orders
    PageResponse<AdminOrderResponse> getAllOrders(OrderStatus status, Long categoryId, Pageable pageable);

    PageResponse<AdminOrderResponse> getDisputedOrders(Pageable pageable);

    AdminOrderResponse getOrderById(Long orderId);

    void resolveDispute(Long orderId, boolean favorClient, String resolution);

    void deleteOrder(Long orderId);

    // Categories
    CategoryResponse createCategory(CategoryCreateRequest request);

    CategoryResponse updateCategory(Long categoryId, CategoryCreateRequest request);

    void deleteCategory(Long categoryId);

    // Reviews
    PageResponse<AdminReviewResponse> getPendingReviews(Pageable pageable);

    PageResponse<AdminReviewResponse> getAllReviews(Boolean moderated, Pageable pageable);

    void approveReview(Long reviewId);

    void rejectReview(Long reviewId, String reason);

    void deleteReview(Long reviewId);

    // Stats
    AdminStatsResponse getOverviewStats();

    // Analytics
    AnalyticsResponse getAnalytics();

    byte[] exportAnalyticsCsv();
}
