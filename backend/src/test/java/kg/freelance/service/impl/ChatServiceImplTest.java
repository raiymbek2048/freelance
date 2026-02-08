package kg.freelance.service.impl;

import kg.freelance.dto.request.ChatMessageRequest;
import kg.freelance.dto.response.ChatRoomResponse;
import kg.freelance.dto.response.MessageResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.entity.ChatRoom;
import kg.freelance.entity.Message;
import kg.freelance.entity.Order;
import kg.freelance.entity.User;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.ForbiddenException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.ChatRoomRepository;
import kg.freelance.repository.MessageRepository;
import kg.freelance.repository.OrderRepository;
import kg.freelance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Tests")
class ChatServiceImplTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User client;
    private User executor;
    private Order order;
    private ChatRoom chatRoom;
    private Message message;

    @BeforeEach
    void setUp() {
        client = User.builder()
                .id(1L)
                .email("client@example.com")
                .fullName("Client User")
                .role(UserRole.USER)
                .active(true)
                .build();

        executor = User.builder()
                .id(2L)
                .email("executor@example.com")
                .fullName("Executor User")
                .role(UserRole.USER)
                .active(true)
                .executorVerified(true)
                .build();

        order = Order.builder()
                .id(1L)
                .title("Test Order")
                .description("Test description")
                .client(client)
                .executor(executor)
                .status(OrderStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();

        chatRoom = ChatRoom.builder()
                .id(1L)
                .order(order)
                .client(client)
                .executor(executor)
                .createdAt(LocalDateTime.now())
                .build();

        message = Message.builder()
                .id(1L)
                .chatRoom(chatRoom)
                .sender(client)
                .content("Hello!")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Get Chat Rooms Tests")
    class GetChatRoomsTests {

        @Test
        @DisplayName("Should return user chat rooms")
        void shouldReturnUserChatRooms() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ChatRoom> page = new PageImpl<>(List.of(chatRoom), pageable, 1);

            when(chatRoomRepository.findByUserId(1L, pageable)).thenReturn(page);
            when(messageRepository.findLastMessageByChatRoomId(1L)).thenReturn(message);
            when(messageRepository.countUnreadMessages(1L, 1L)).thenReturn(0L);

            // When
            PageResponse<ChatRoomResponse> result = chatService.getMyChatRooms(1L, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getOrderTitle()).isEqualTo("Test Order");
        }
    }

    @Nested
    @DisplayName("Get Chat Room Tests")
    class GetChatRoomTests {

        @Test
        @DisplayName("Should return chat room by ID for participant")
        void shouldReturnChatRoomByIdForParticipant() {
            // Given
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
            when(messageRepository.findLastMessageByChatRoomId(1L)).thenReturn(message);
            when(messageRepository.countUnreadMessages(1L, 1L)).thenReturn(0L);

            // When
            ChatRoomResponse result = chatService.getChatRoomById(1L, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getOrderTitle()).isEqualTo("Test Order");
        }

        @Test
        @DisplayName("Should throw exception when chat room not found")
        void shouldThrowExceptionWhenChatRoomNotFound() {
            // Given
            when(chatRoomRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> chatService.getChatRoomById(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when user is not participant")
        void shouldThrowExceptionWhenUserNotParticipant() {
            // Given
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));

            // When/Then - User 999 is not a participant
            assertThatThrownBy(() -> chatService.getChatRoomById(1L, 999L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You are not a participant of this chat");
        }

        @Test
        @DisplayName("Should return chat room by order ID")
        void shouldReturnChatRoomByOrderId() {
            // Given
            when(chatRoomRepository.findByOrderId(1L)).thenReturn(Optional.of(chatRoom));
            when(messageRepository.findLastMessageByChatRoomId(1L)).thenReturn(message);
            when(messageRepository.countUnreadMessages(1L, 1L)).thenReturn(0L);

            // When
            ChatRoomResponse result = chatService.getChatRoomByOrderId(1L, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Get Or Create Chat Room Tests")
    class GetOrCreateChatRoomTests {

        @Test
        @DisplayName("Should return existing chat room")
        void shouldReturnExistingChatRoom() {
            // Given
            when(chatRoomRepository.findByOrderIdAndExecutorId(1L, 2L)).thenReturn(Optional.of(chatRoom));
            when(messageRepository.findLastMessageByChatRoomId(1L)).thenReturn(null);
            when(messageRepository.countUnreadMessages(1L, 1L)).thenReturn(0L);

            // When
            ChatRoomResponse result = chatService.getOrCreateChatRoom(1L, 2L, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        }

        @Test
        @DisplayName("Should create new chat room when not exists")
        void shouldCreateNewChatRoomWhenNotExists() {
            // Given
            when(chatRoomRepository.findByOrderIdAndExecutorId(1L, 2L)).thenReturn(Optional.empty());
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(userRepository.findById(2L)).thenReturn(Optional.of(executor));
            when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);
            when(messageRepository.findLastMessageByChatRoomId(1L)).thenReturn(null);
            when(messageRepository.countUnreadMessages(1L, 1L)).thenReturn(0L);

            // When
            ChatRoomResponse result = chatService.getOrCreateChatRoom(1L, 2L, 1L);

            // Then
            assertThat(result).isNotNull();
            verify(chatRoomRepository).save(any(ChatRoom.class));
        }

        @Test
        @DisplayName("Should throw exception when user is not order owner")
        void shouldThrowExceptionWhenUserNotOrderOwner() {
            // Given
            when(chatRoomRepository.findByOrderIdAndExecutorId(1L, 2L)).thenReturn(Optional.empty());
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(userRepository.findById(2L)).thenReturn(Optional.of(executor));

            // When/Then - User 999 is not the owner of the order
            assertThatThrownBy(() -> chatService.getOrCreateChatRoom(1L, 2L, 999L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You are not the owner of this order");
        }
    }

    @Nested
    @DisplayName("Get Messages Tests")
    class GetMessagesTests {

        @Test
        @DisplayName("Should return chat messages")
        void shouldReturnChatMessages() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Page<Message> page = new PageImpl<>(List.of(message), pageable, 1);

            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
            when(messageRepository.findByChatRoomIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);

            // When
            PageResponse<MessageResponse> result = chatService.getChatMessages(1L, 1L, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getContent()).isEqualTo("Hello!");
        }
    }

    @Nested
    @DisplayName("Send Message Tests")
    class SendMessageTests {

        @Test
        @DisplayName("Should send message successfully")
        void shouldSendMessageSuccessfully() {
            // Given
            ChatMessageRequest request = new ChatMessageRequest();
            request.setContent("Hello, executor!");

            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
            when(userRepository.findById(1L)).thenReturn(Optional.of(client));
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
                Message msg = invocation.getArgument(0);
                msg.setId(2L);
                msg.setCreatedAt(LocalDateTime.now());
                return msg;
            });
            when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);

            // When
            MessageResponse result = chatService.sendMessage(1L, 1L, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("Hello, executor!");
            assertThat(result.getIsMine()).isTrue();
            verify(messageRepository).save(any(Message.class));
            verify(messagingTemplate).convertAndSendToUser(
                    eq("executor@example.com"),
                    eq("/queue/messages"),
                    any()
            );
        }

        @Test
        @DisplayName("Should throw exception when sender is not participant")
        void shouldThrowExceptionWhenSenderNotParticipant() {
            // Given
            ChatMessageRequest request = new ChatMessageRequest();
            request.setContent("Hello!");

            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));

            // When/Then
            assertThatThrownBy(() -> chatService.sendMessage(1L, 999L, request))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("Mark Messages As Read Tests")
    class MarkMessagesAsReadTests {

        @Test
        @DisplayName("Should mark messages as read")
        void shouldMarkMessagesAsRead() {
            // Given
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));

            // When
            chatService.markMessagesAsRead(1L, 1L);

            // Then
            verify(messageRepository).markMessagesAsRead(1L, 1L);
        }
    }

    @Nested
    @DisplayName("Participant Helper Tests")
    class ParticipantHelperTests {

        @Test
        @DisplayName("Should check if user is participant")
        void shouldCheckIfUserIsParticipant() {
            // Given
            when(chatRoomRepository.isUserParticipant(1L, 1L)).thenReturn(true);
            when(chatRoomRepository.isUserParticipant(1L, 999L)).thenReturn(false);

            // When/Then
            assertThat(chatService.isUserParticipant(1L, 1L)).isTrue();
            assertThat(chatService.isUserParticipant(1L, 999L)).isFalse();
        }

        @Test
        @DisplayName("Should get other participant ID")
        void shouldGetOtherParticipantId() {
            // Given
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));

            // When
            Long otherIdForClient = chatService.getOtherParticipantId(1L, 1L);
            Long otherIdForExecutor = chatService.getOtherParticipantId(1L, 2L);

            // Then
            assertThat(otherIdForClient).isEqualTo(2L); // Executor
            assertThat(otherIdForExecutor).isEqualTo(1L); // Client
        }

        @Test
        @DisplayName("Should get other participant email")
        void shouldGetOtherParticipantEmail() {
            // Given
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));

            // When
            String emailForClient = chatService.getOtherParticipantEmail(1L, 1L);
            String emailForExecutor = chatService.getOtherParticipantEmail(1L, 2L);

            // Then
            assertThat(emailForClient).isEqualTo("executor@example.com");
            assertThat(emailForExecutor).isEqualTo("client@example.com");
        }
    }
}
