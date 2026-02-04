package kg.freelance.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicator {

    private Long chatRoomId;
    private Long userId;
    private String userName;
    private Boolean isTyping;
}
