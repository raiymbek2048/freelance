package kg.freelance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class VerificationSubmitRequest {

    @NotBlank(message = "Passport photo URL is required")
    private String passportUrl;

    @NotBlank(message = "Selfie photo URL is required")
    private String selfieUrl;

    @NotEmpty(message = "At least one category must be selected")
    private List<Long> categoryIds;
}
