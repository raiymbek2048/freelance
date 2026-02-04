package kg.freelance.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SelectExecutorRequest {

    @NotNull(message = "Response ID is required")
    private Long responseId;

    @Positive(message = "Agreed price must be positive")
    private BigDecimal agreedPrice;

    private LocalDate agreedDeadline;
}
