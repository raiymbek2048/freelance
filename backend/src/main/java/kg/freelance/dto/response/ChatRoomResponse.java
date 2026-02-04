package kg.freelance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatRoomResponse {

    private Long id;
    private Long orderId;
    private String orderTitle;

    // Other participant info
    private Long participantId;
    private String participantName;
    private String participantAvatarUrl;

    // Last message preview
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Long lastMessageSenderId;

    // Unread count
    private Long unreadCount;

    private LocalDateTime createdAt;
}
