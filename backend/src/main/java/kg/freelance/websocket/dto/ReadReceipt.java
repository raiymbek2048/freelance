package kg.freelance.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceipt {

    private Long chatRoomId;
    private Long userId;
    private LocalDateTime readAt;
}
