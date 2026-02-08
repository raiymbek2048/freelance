package kg.freelance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AnalyticsResponse {

    // Time period stats
    private List<DailyStats> dailyStats;      // Last 30 days
    private List<WeeklyStats> weeklyStats;    // Last 12 weeks
    private List<MonthlyStats> monthlyStats;  // Last 12 months

    // Subscription revenue
    private SubscriptionAnalytics subscriptions;

    // Conversion rates
    private ConversionStats conversions;

    @Data
    @Builder
    public static class DailyStats {
        private LocalDate date;
        private Long newUsers;
        private Long newOrders;
        private Long completedOrders;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    public static class WeeklyStats {
        private LocalDate weekStart;
        private Long newUsers;
        private Long newOrders;
        private Long completedOrders;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    public static class MonthlyStats {
        private Integer year;
        private Integer month;
        private String monthName;
        private Long newUsers;
        private Long newOrders;
        private Long completedOrders;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    public static class SubscriptionAnalytics {
        private Long totalSubscriptions;
        private Long activeSubscriptions;
        private Long trialSubscriptions;
        private Long expiredSubscriptions;
        private BigDecimal totalRevenue;
        private BigDecimal revenueThisMonth;
        private BigDecimal revenueLastMonth;
        private List<SubscriptionByPeriod> byPeriod;
    }

    @Data
    @Builder
    public static class SubscriptionByPeriod {
        private LocalDate date;
        private Long newSubscriptions;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    public static class ConversionStats {
        private Double registrationToExecutorRate;  // % users who became executors
        private Double executorToVerifiedRate;      // % executors who got verified
        private Double orderCompletionRate;         // % orders completed
        private Double responseToSelectionRate;     // % responses that got selected
    }
}
