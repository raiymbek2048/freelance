package kg.freelance.dto.response;

import kg.freelance.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDetailResponse {

    private Long id;
    private String title;
    private String description;
    private Long categoryId;
    private String categoryName;

    // Client info
    private Long clientId;
    private String clientName;
    private String clientAvatarUrl;

    // Executor info (if assigned)
    private Long executorId;
    private String executorName;
    private String executorAvatarUrl;

    // Budget
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private BigDecimal agreedPrice;

    // Location
    private String location;

    // Dates
    private LocalDate deadline;
    private LocalDate agreedDeadline;

    // Status
    private OrderStatus status;
    private Boolean isPublic;

    // Stats
    private Integer viewCount;
    private Integer responseCount;

    // Attachments
    private List<String> attachments;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // User context
    private Boolean isOwner;
    private Boolean isExecutor;
    private Boolean hasResponded;

    // Verification context
    private Boolean descriptionTruncated;
    private Boolean requiresVerification;
}
