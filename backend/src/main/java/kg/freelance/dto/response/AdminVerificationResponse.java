package kg.freelance.dto.response;

import kg.freelance.entity.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminVerificationResponse {

    private Long userId;
    private String userFullName;
    private String userEmail;
    private String userPhone;
    private String userAvatarUrl;

    private VerificationStatus status;
    private String passportUrl;
    private String selfieUrl;
    private String rejectionReason;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String reviewedByName;
}
