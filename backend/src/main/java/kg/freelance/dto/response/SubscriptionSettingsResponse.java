package kg.freelance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionSettingsResponse {

    private BigDecimal price;
    private LocalDate subscriptionStartDate;
    private Integer trialDays;
    private String announcementMessage;
    private Boolean announcementEnabled;
    private LocalDateTime updatedAt;
}
