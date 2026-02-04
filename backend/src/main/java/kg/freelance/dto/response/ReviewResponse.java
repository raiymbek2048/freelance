package kg.freelance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {

    private Long id;
    private Long orderId;
    private String orderTitle;
    private Long clientId;
    private String clientName;
    private String clientAvatarUrl;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
