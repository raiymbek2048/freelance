package kg.freelance.dto.response;

import kg.freelance.entity.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private Long orderId;
    private String link;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
