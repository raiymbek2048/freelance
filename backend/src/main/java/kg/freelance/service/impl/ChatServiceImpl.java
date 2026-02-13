package kg.freelance.service.impl;

import kg.freelance.dto.request.ChatMessageRequest;
import kg.freelance.dto.response.ChatRoomResponse;
import kg.freelance.dto.response.MessageResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.entity.ChatRoom;
import kg.freelance.entity.Message;
import kg.freelance.entity.User;
import kg.freelance.exception.ForbiddenException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.entity.Order;
import kg.freelance.repository.ChatRoomRepository;
import kg.freelance.repository.MessageRepository;
import kg.freelance.repository.OrderRepository;
import kg.freelance.repository.UserRepository;
import kg.freelance.service.ChatService;
import kg.freelance.websocket.dto.WsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ChatRoomResponse> getMyChatRooms(Long userId, Pageable pageable) {
        Page<ChatRoom> page = chatRoomRepository.findByUserId(userId, pageable);

        List<ChatRoomResponse> content = page.getContent().stream()
                .map(room -> mapToChatRoomResponse(room, userId))
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoomResponse getChatRoomById(Long chatRoomId, Long userId) {
        ChatRoom room = getChatRoomEntity(chatRoomId);
        validateParticipant(room, userId);
        return mapToChatRoomResponse(room, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoomResponse getChatRoomByOrderId(Long orderId, Long userId) {
        ChatRoom room = chatRoomRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatRoom", "orderId", orderId));
        validateParticipant(room, userId);
        return mapToChatRoomResponse(room, userId);
    }

    @Override
    @Transactional
    public ChatRoomResponse getOrCreateChatRoom(Long orderId, Long executorId, Long clientId) {
        // Check if chat room already exists
        return chatRoomRepository.findByOrderIdAndExecutorId(orderId, executorId)
                .map(room -> mapToChatRoomResponse(room, clientId))
                .orElseGet(() -> {
                    // Create new chat room
                    Order order = orderRepository.findById(orderId)
                            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

                    User executor = userRepository.findById(executorId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", executorId));

                    // Verify that the client owns the order
                    if (!order.getClient().getId().equals(clientId)) {
                        throw new ForbiddenException("You are not the owner of this order");
                    }

                    try {
                        ChatRoom newRoom = ChatRoom.builder()
                                .order(order)
                                .client(order.getClient())
                                .executor(executor)
                                .build();

                        newRoom = chatRoomRepository.save(newRoom);
                        return mapToChatRoomResponse(newRoom, clientId);
                    } catch (DataIntegrityViolationException e) {
                        // Race condition: another thread created the room first
                        return chatRoomRepository.findByOrderIdAndExecutorId(orderId, executorId)
                                .map(room -> mapToChatRoomResponse(room, clientId))
                                .orElseThrow(() -> new ResourceNotFoundException("ChatRoom", "orderId", orderId));
                    }
                });
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoom getChatRoomEntity(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatRoom", "id", chatRoomId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getChatMessages(Long chatRoomId, Long userId, Pageable pageable) {
        ChatRoom room = getChatRoomEntity(chatRoomId);
        validateParticipant(room, userId);

        Page<Message> page = messageRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoomId, pageable);

        List<MessageResponse> content = page.getContent().stream()
                .map(msg -> mapToMessageResponse(msg, userId))
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long chatRoomId, Long senderId, ChatMessageRequest request) {
        ChatRoom room = getChatRoomEntity(chatRoomId);
        validateParticipant(room, senderId);

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));

        Message message = Message.builder()
                .chatRoom(room)
                .sender(sender)
                .content(request.getContent())
                .attachments(request.getAttachments())
                .isRead(false)
                .build();

        message = messageRepository.save(message);

        // Update last message time
        room.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        // Send WebSocket notification to the other participant
        User otherUser = getOtherParticipant(room, senderId);
        WsMessage wsMessage = WsMessage.builder()
                .id(message.getId())
                .chatRoomId(chatRoomId)
                .senderId(sender.getId())
                .senderName(sender.getFullName())
                .senderAvatarUrl(sender.getAvatarUrl())
                .content(message.getContent())
                .attachments(message.getAttachments())
                .createdAt(message.getCreatedAt())
                .type(WsMessage.MessageType.CHAT)
                .build();

        // Use email as user identifier (matches principal.getName())
        messagingTemplate.convertAndSendToUser(
                otherUser.getEmail(),
                "/queue/messages",
                wsMessage
        );

        return mapToMessageResponse(message, senderId);
    }

    @Override
    @Transactional
    public WsMessage sendMessageWs(Long chatRoomId, Long senderId, ChatMessageRequest request) {
        ChatRoom room = getChatRoomEntity(chatRoomId);
        validateParticipant(room, senderId);

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));

        Message message = Message.builder()
                .chatRoom(room)
                .sender(sender)
                .content(request.getContent())
                .attachments(request.getAttachments())
                .isRead(false)
                .build();

        message = messageRepository.save(message);

        // Update last message time
        room.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        return WsMessage.builder()
                .id(message.getId())
                .chatRoomId(chatRoomId)
                .senderId(sender.getId())
                .senderName(sender.getFullName())
                .senderAvatarUrl(sender.getAvatarUrl())
                .content(message.getContent())
                .attachments(message.getAttachments())
                .createdAt(message.getCreatedAt())
                .type(WsMessage.MessageType.CHAT)
                .build();
    }

    @Override
    @Transactional
    public void markMessagesAsRead(Long chatRoomId, Long userId) {
        ChatRoom room = getChatRoomEntity(chatRoomId);
        validateParticipant(room, userId);

        messageRepository.markMessagesAsRead(chatRoomId, userId);
    }

    @Override
    public boolean isUserParticipant(Long chatRoomId, Long userId) {
        return chatRoomRepository.isUserParticipant(chatRoomId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getOtherParticipantId(Long chatRoomId, Long userId) {
        ChatRoom room = getChatRoomEntity(chatRoomId);
        return room.getClient().getId().equals(userId)
                ? room.getExecutor().getId()
                : room.getClient().getId();
    }

    @Override
    @Transactional(readOnly = true)
    public String getOtherParticipantEmail(Long chatRoomId, Long userId) {
        ChatRoom room = getChatRoomEntity(chatRoomId);
        return room.getClient().getId().equals(userId)
                ? room.getExecutor().getEmail()
                : room.getClient().getEmail();
    }

    // Helper methods

    private void validateParticipant(ChatRoom room, Long userId) {
        if (!room.getClient().getId().equals(userId) && !room.getExecutor().getId().equals(userId)) {
            throw new ForbiddenException("You are not a participant of this chat");
        }
    }

    private User getOtherParticipant(ChatRoom room, Long userId) {
        return room.getClient().getId().equals(userId)
                ? room.getExecutor()
                : room.getClient();
    }

    private ChatRoomResponse mapToChatRoomResponse(ChatRoom room, Long userId) {
        User otherParticipant = room.getClient().getId().equals(userId)
                ? room.getExecutor()
                : room.getClient();

        // Get last message
        String lastMessage = null;
        Long lastMessageSenderId = null;
        Message last = messageRepository.findLastMessageByChatRoomId(room.getId());
        if (last != null) {
            lastMessage = last.getContent();
            lastMessageSenderId = last.getSender().getId();
        }

        // Get unread count
        long unreadCount = messageRepository.countUnreadMessages(room.getId(), userId);

        return ChatRoomResponse.builder()
                .id(room.getId())
                .orderId(room.getOrder().getId())
                .orderTitle(room.getOrder().getTitle())
                .participantId(otherParticipant.getId())
                .participantName(otherParticipant.getFullName())
                .participantAvatarUrl(otherParticipant.getAvatarUrl())
                .lastMessage(lastMessage)
                .lastMessageAt(room.getLastMessageAt())
                .lastMessageSenderId(lastMessageSenderId)
                .unreadCount(unreadCount)
                .createdAt(room.getCreatedAt())
                .build();
    }

    private MessageResponse mapToMessageResponse(Message message, Long currentUserId) {
        User sender = message.getSender();
        return MessageResponse.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .senderId(sender.getId())
                .senderName(sender.getFullName())
                .senderAvatarUrl(sender.getAvatarUrl())
                .content(message.getContent())
                .attachments(message.getAttachments())
                .isRead(message.getIsRead())
                .isMine(sender.getId().equals(currentUserId))
                .createdAt(message.getCreatedAt())
                .build();
    }
}
