package kg.freelance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ExecutorListResponse {

    private Long id;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private String specialization;
    private Integer completedOrders;
    private BigDecimal rating;
    private Integer reviewCount;
    private Boolean availableForWork;
    private List<CategoryResponse> categories;
    private String reputationLevel;
    private String reputationColor;
}
