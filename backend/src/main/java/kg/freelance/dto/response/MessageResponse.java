package kg.freelance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MessageResponse {

    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String senderName;
    private String senderAvatarUrl;
    private String content;
    private List<String> attachments;
    private Boolean isRead;
    private Boolean isMine;
    private LocalDateTime createdAt;
}
