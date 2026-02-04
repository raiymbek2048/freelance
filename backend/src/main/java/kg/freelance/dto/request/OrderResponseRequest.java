package kg.freelance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderResponseRequest {

    @NotBlank(message = "Cover letter is required")
    @Size(max = 2000, message = "Cover letter must not exceed 2000 characters")
    private String coverLetter;

    @Positive(message = "Proposed price must be positive")
    private BigDecimal proposedPrice;

    @Positive(message = "Proposed days must be positive")
    private Integer proposedDays;
}
