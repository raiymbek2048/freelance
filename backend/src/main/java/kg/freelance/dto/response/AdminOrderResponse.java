package kg.freelance.dto.response;

import kg.freelance.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminOrderResponse {

    private Long id;
    private String title;
    private String description;
    private Long categoryId;
    private String categoryName;

    // Client
    private Long clientId;
    private String clientName;
    private String clientEmail;

    // Executor
    private Long executorId;
    private String executorName;
    private String executorEmail;

    // Financial
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private BigDecimal agreedPrice;

    // Dates
    private LocalDate deadline;
    private LocalDate agreedDeadline;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // Status
    private OrderStatus status;
    private Boolean isPublic;
    private Integer responseCount;
    private Integer viewCount;

    // Review
    private Boolean hasReview;
    private Integer reviewRating;
}
