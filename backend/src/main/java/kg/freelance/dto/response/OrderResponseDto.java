package kg.freelance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderResponseDto {

    private Long id;
    private Long orderId;
    private Long executorId;
    private String executorName;
    private String executorAvatarUrl;
    private String executorSpecialization;
    private BigDecimal executorRating;
    private Integer executorCompletedOrders;
    private String coverLetter;
    private BigDecimal proposedPrice;
    private Integer proposedDays;
    private Boolean isSelected;
    private LocalDateTime createdAt;
}
