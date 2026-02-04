package kg.freelance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @Positive(message = "Minimum budget must be positive")
    private BigDecimal budgetMin;

    @Positive(message = "Maximum budget must be positive")
    private BigDecimal budgetMax;

    private LocalDate deadline;

    private Boolean isPublic = true;

    private List<String> attachments;
}
