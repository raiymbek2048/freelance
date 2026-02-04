package kg.freelance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminReviewResponse {

    private Long id;
    private Long orderId;
    private String orderTitle;

    private Long clientId;
    private String clientName;
    private String clientEmail;

    private Long executorId;
    private String executorName;
    private String executorEmail;

    private Integer rating;
    private String comment;
    private Boolean isModerated;
    private Boolean isVisible;
    private String moderatorComment;

    private LocalDateTime createdAt;
}
