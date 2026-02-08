package kg.freelance.dto.response;

import kg.freelance.entity.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserSubscriptionResponse {

    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer daysRemaining;
    private Boolean isActive;
}
