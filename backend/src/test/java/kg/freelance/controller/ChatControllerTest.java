package kg.freelance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kg.freelance.dto.request.ChatMessageRequest;
import kg.freelance.dto.response.ChatRoomResponse;
import kg.freelance.dto.response.MessageResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.ForbiddenException;
import kg.freelance.exception.GlobalExceptionHandler;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController Tests")
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        userPrincipal = UserPrincipal.builder()
                .id(1L).email("test@example.com").fullName("Test User")
                .role(UserRole.USER).active(true).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities()));
    }

    @Nested
    @DisplayName("GET /api/v1/chats")
    class GetMyChatRoomsTests {

        @Test
        @DisplayName("Should return my chat rooms")
        void shouldReturnMyChatRooms() throws Exception {
            PageResponse<ChatRoomResponse> page = PageResponse.<ChatRoomResponse>builder()
                    .content(List.of()).page(0).size(20)
                    .totalElements(0).totalPages(0).first(true).last(true).build();

            when(chatService.getMyChatRooms(eq(1L), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/chats"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/chats/{id}")
    class GetChatRoomTests {

        @Test
        @DisplayName("Should return chat room by ID")
        void shouldReturnChatRoom() throws Exception {
            ChatRoomResponse room = ChatRoomResponse.builder().id(1L).build();
            when(chatService.getChatRoomById(1L, 1L)).thenReturn(room);

            mockMvc.perform(get("/api/v1/chats/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("Should return 403 when not participant")
        void shouldReturn403WhenNotParticipant() throws Exception {
            when(chatService.getChatRoomById(1L, 1L))
                    .thenThrow(new ForbiddenException("Access denied"));

            mockMvc.perform(get("/api/v1/chats/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/chats/order/{orderId}")
    class GetChatRoomByOrderTests {

        @Test
        @DisplayName("Should return chat room by order")
        void shouldReturnChatRoomByOrder() throws Exception {
            ChatRoomResponse room = ChatRoomResponse.builder().id(1L).build();
            when(chatService.getChatRoomByOrderId(100L, 1L)).thenReturn(room);

            mockMvc.perform(get("/api/v1/chats/order/100"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/chats/order/{orderId}/with/{executorId}")
    class GetOrCreateChatRoomTests {

        @Test
        @DisplayName("Should get or create chat room")
        void shouldGetOrCreateChatRoom() throws Exception {
            ChatRoomResponse room = ChatRoomResponse.builder().id(1L).build();
            when(chatService.getOrCreateChatRoom(100L, 2L, 1L)).thenReturn(room);

            mockMvc.perform(post("/api/v1/chats/order/100/with/2"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/chats/{id}/messages")
    class GetMessagesTests {

        @Test
        @DisplayName("Should return messages")
        void shouldReturnMessages() throws Exception {
            PageResponse<MessageResponse> page = PageResponse.<MessageResponse>builder()
                    .content(List.of()).page(0).size(50)
                    .totalElements(0).totalPages(0).first(true).last(true).build();

            when(chatService.getChatMessages(eq(1L), eq(1L), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/chats/1/messages"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/chats/{id}/messages")
    class SendMessageTests {

        @Test
        @DisplayName("Should send message")
        void shouldSendMessage() throws Exception {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setContent("Hello!");

            MessageResponse response = MessageResponse.builder()
                    .id(1L).content("Hello!").build();

            when(chatService.sendMessage(eq(1L), eq(1L), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/chats/1/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value("Hello!"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/chats/{id}/messages/read")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark messages as read")
        void shouldMarkAsRead() throws Exception {
            doNothing().when(chatService).markMessagesAsRead(1L, 1L);

            mockMvc.perform(put("/api/v1/chats/1/messages/read"))
                    .andExpect(status().isOk());
        }
    }
}
