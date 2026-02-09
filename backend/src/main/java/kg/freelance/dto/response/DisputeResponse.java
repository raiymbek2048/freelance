package kg.freelance.dto.response;

import kg.freelance.entity.enums.DisputeResolution;
import kg.freelance.entity.enums.DisputeStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DisputeResponse {

    private Long id;
    private Long orderId;
    private String orderTitle;

    private Long openedById;
    private String openedByName;
    private String openedByRole;

    private Long clientId;
    private String clientName;
    private String clientEmail;
    private String clientAvatarUrl;

    private Long executorId;
    private String executorName;
    private String executorEmail;
    private String executorAvatarUrl;

    private String reason;
    private DisputeStatus status;

    private Long adminId;
    private String adminName;
    private String adminNotes;

    private DisputeResolution resolution;
    private String resolutionNotes;

    private Long chatRoomId;

    private List<DisputeEvidenceResponse> evidence;
    private int evidenceCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
