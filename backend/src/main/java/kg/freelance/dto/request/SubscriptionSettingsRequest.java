package kg.freelance.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SubscriptionSettingsRequest {

    @DecimalMin(value = "0.0", message = "Price must be non-negative")
    private BigDecimal price;

    private LocalDate subscriptionStartDate;

    @Min(value = 0, message = "Trial days must be non-negative")
    private Integer trialDays;

    @Size(max = 1000, message = "Announcement message must not exceed 1000 characters")
    private String announcementMessage;

    private Boolean announcementEnabled;
}
