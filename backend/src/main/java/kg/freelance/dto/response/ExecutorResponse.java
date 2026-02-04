package kg.freelance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ExecutorResponse {

    private Long id;
    private String fullName;
    private String avatarUrl;
    private String whatsappLink;
    private String bio;
    private String specialization;
    private Integer totalOrders;
    private Integer completedOrders;
    private Double avgCompletionDays;
    private BigDecimal rating;
    private Integer reviewCount;
    private Boolean availableForWork;
    private LocalDateTime lastActiveAt;
    private LocalDateTime memberSince;
    private List<CategoryResponse> categories;
}
