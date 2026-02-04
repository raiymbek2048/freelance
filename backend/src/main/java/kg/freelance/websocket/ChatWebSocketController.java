package kg.freelance.websocket;

import kg.freelance.dto.request.ChatMessageRequest;
import kg.freelance.entity.ChatRoom;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.ChatService;
import kg.freelance.websocket.dto.ReadReceipt;
import kg.freelance.websocket.dto.TypingIndicator;
import kg.freelance.websocket.dto.WsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send a message to chat room
     * Client sends to: /app/chat/{chatRoomId}/send
     */
    @MessageMapping("/chat/{chatRoomId}/send")
    public void sendMessage(
            @DestinationVariable Long chatRoomId,
            @Payload ChatMessageRequest request,
            Principal principal) {

        UserPrincipal user = extractUser(principal);
        if (user == null) {
            log.warn("Unauthorized WebSocket message attempt");
            return;
        }

        if (!chatService.isUserParticipant(chatRoomId, user.getId())) {
            log.warn("User {} is not a participant of chat {}", user.getId(), chatRoomId);
            return;
        }

        // Save and create message
        WsMessage message = chatService.sendMessageWs(chatRoomId, user.getId(), request);

        // Get other participant email
        String otherUserEmail = chatService.getOtherParticipantEmail(chatRoomId, user.getId());

        // Send to both participants (use email as identifier)
        sendToUser(user.getEmail(), message);
        sendToUser(otherUserEmail, message);

        log.debug("Message sent in chat {} by user {}", chatRoomId, user.getId());
    }

    /**
     * Mark messages as read
     * Client sends to: /app/chat/{chatRoomId}/read
     */
    @MessageMapping("/chat/{chatRoomId}/read")
    public void markAsRead(
            @DestinationVariable Long chatRoomId,
            Principal principal) {

        UserPrincipal user = extractUser(principal);
        if (user == null) return;

        if (!chatService.isUserParticipant(chatRoomId, user.getId())) return;

        chatService.markMessagesAsRead(chatRoomId, user.getId());

        // Notify other participant about read receipt
        String otherUserEmail = chatService.getOtherParticipantEmail(chatRoomId, user.getId());

        ReadReceipt receipt = ReadReceipt.builder()
                .chatRoomId(chatRoomId)
                .userId(user.getId())
                .readAt(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSendToUser(
                otherUserEmail,
                "/queue/read-receipts",
                receipt
        );

        log.debug("Read receipt sent for chat {} by user {}", chatRoomId, user.getId());
    }

    /**
     * Typing indicator
     * Client sends to: /app/chat/{chatRoomId}/typing
     */
    @MessageMapping("/chat/{chatRoomId}/typing")
    public void typing(
            @DestinationVariable Long chatRoomId,
            @Payload TypingIndicator indicator,
            Principal principal) {

        UserPrincipal user = extractUser(principal);
        if (user == null) return;

        if (!chatService.isUserParticipant(chatRoomId, user.getId())) return;

        String otherUserEmail = chatService.getOtherParticipantEmail(chatRoomId, user.getId());

        TypingIndicator notification = TypingIndicator.builder()
                .chatRoomId(chatRoomId)
                .userId(user.getId())
                .userName(user.getFullName())
                .isTyping(indicator.getIsTyping())
                .build();

        messagingTemplate.convertAndSendToUser(
                otherUserEmail,
                "/queue/typing",
                notification
        );
    }

    // Helper methods

    private UserPrincipal extractUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            Object p = auth.getPrincipal();
            if (p instanceof UserPrincipal) {
                return (UserPrincipal) p;
            }
        }
        return null;
    }

    private void sendToUser(String userEmail, WsMessage message) {
        messagingTemplate.convertAndSendToUser(
                userEmail,
                "/queue/messages",
                message
        );
    }
}
