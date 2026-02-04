package kg.freelance.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsMessage {

    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String senderName;
    private String senderAvatarUrl;
    private String content;
    private List<String> attachments;
    private LocalDateTime createdAt;

    // Type of WebSocket message
    private MessageType type;

    public enum MessageType {
        CHAT,               // Regular chat message
        SYSTEM,             // System notification message
        READ_RECEIPT,       // Message read notification
        TYPING,             // Typing indicator
        USER_JOINED,        // User joined chat
        USER_LEFT,          // User left chat
        ADMIN_NOTIFICATION  // Admin notification (disputes, etc.)
    }
}
