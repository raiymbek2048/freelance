package kg.freelance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenDisputeRequest {

    @NotBlank(message = "Reason is required")
    @Size(min = 10, max = 2000, message = "Reason must be between 10 and 2000 characters")
    private String reason;
}
