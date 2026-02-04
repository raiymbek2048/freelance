package kg.freelance.service;

import kg.freelance.dto.request.ChatMessageRequest;
import kg.freelance.dto.response.ChatRoomResponse;
import kg.freelance.dto.response.MessageResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.entity.ChatRoom;
import kg.freelance.websocket.dto.WsMessage;
import org.springframework.data.domain.Pageable;

public interface ChatService {

    // Chat rooms
    PageResponse<ChatRoomResponse> getMyChatRooms(Long userId, Pageable pageable);

    ChatRoomResponse getChatRoomById(Long chatRoomId, Long userId);

    ChatRoomResponse getChatRoomByOrderId(Long orderId, Long userId);

    ChatRoomResponse getOrCreateChatRoom(Long orderId, Long executorId, Long clientId);

    ChatRoom getChatRoomEntity(Long chatRoomId);

    // Messages
    PageResponse<MessageResponse> getChatMessages(Long chatRoomId, Long userId, Pageable pageable);

    MessageResponse sendMessage(Long chatRoomId, Long senderId, ChatMessageRequest request);

    WsMessage sendMessageWs(Long chatRoomId, Long senderId, ChatMessageRequest request);

    void markMessagesAsRead(Long chatRoomId, Long userId);

    // Helpers
    boolean isUserParticipant(Long chatRoomId, Long userId);

    Long getOtherParticipantId(Long chatRoomId, Long userId);

    String getOtherParticipantEmail(Long chatRoomId, Long userId);
}
