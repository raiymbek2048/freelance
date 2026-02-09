package kg.freelance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DisputeEvidenceResponse {

    private Long id;
    private Long uploadedById;
    private String uploadedByName;
    private String uploadedByRole;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String description;
    private LocalDateTime createdAt;
}
