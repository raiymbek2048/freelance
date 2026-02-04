package kg.freelance.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerificationSubmitRequest {

    @NotBlank(message = "Passport photo URL is required")
    private String passportUrl;

    @NotBlank(message = "Selfie photo URL is required")
    private String selfieUrl;
}
