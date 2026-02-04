package kg.freelance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AdminStatsResponse {

    // Users
    private Long totalUsers;
    private Long activeUsers;
    private Long executors;
    private Long newUsersToday;
    private Long newUsersThisWeek;
    private Long newUsersThisMonth;

    // Orders
    private Long totalOrders;
    private Long newOrders;
    private Long inProgressOrders;
    private Long completedOrders;
    private Long disputedOrders;
    private Long cancelledOrders;

    // Financial
    private BigDecimal totalOrdersValue;
    private BigDecimal averageOrderValue;

    // Reviews
    private Long totalReviews;
    private Long pendingModeration;
    private Double averageRating;

    // Categories
    private Long totalCategories;
    private List<CategoryStats> topCategories;

    @Data
    @Builder
    public static class CategoryStats {
        private Long categoryId;
        private String categoryName;
        private Long orderCount;
        private Long executorCount;
    }
}
