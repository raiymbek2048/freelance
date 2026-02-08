package kg.freelance.dto.response;

import kg.freelance.entity.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MySubscriptionResponse {

    private Boolean hasActiveSubscription;
    private Boolean subscriptionRequired;
    private SubscriptionStatus status;
    private LocalDateTime endDate;
    private Integer daysRemaining;
    private BigDecimal price;
    private Boolean canStartTrial;
    private Integer trialDays;
}
