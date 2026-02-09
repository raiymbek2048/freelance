package kg.freelance.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolveDisputeRequest {

    @NotNull(message = "favorClient is required")
    private Boolean favorClient;

    private String resolutionNotes;

    private String adminNotes;
}
