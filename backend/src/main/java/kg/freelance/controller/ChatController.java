package kg.freelance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.freelance.dto.request.ChatMessageRequest;
import kg.freelance.dto.response.ChatRoomResponse;
import kg.freelance.dto.response.MessageResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat management (REST fallback)")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    @Operation(summary = "Get my chat rooms", description = "Get all chat rooms for current user")
    public ResponseEntity<PageResponse<ChatRoomResponse>> getMyChatRooms(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ChatRoomResponse> response = chatService.getMyChatRooms(user.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get chat room", description = "Get chat room by ID")
    public ResponseEntity<ChatRoomResponse> getChatRoom(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {

        ChatRoomResponse response = chatService.getChatRoomById(id, user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get chat room by order", description = "Get chat room by order ID")
    public ResponseEntity<ChatRoomResponse> getChatRoomByOrder(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long orderId) {

        ChatRoomResponse response = chatService.getChatRoomByOrderId(orderId, user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/order/{orderId}/with/{executorId}")
    @Operation(summary = "Get or create chat room", description = "Get or create chat room for an order with a specific executor")
    public ResponseEntity<ChatRoomResponse> getOrCreateChatRoom(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long orderId,
            @PathVariable Long executorId) {

        ChatRoomResponse response = chatService.getOrCreateChatRoom(orderId, executorId, user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get messages", description = "Get messages for a chat room")
    public ResponseEntity<PageResponse<MessageResponse>> getMessages(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<MessageResponse> response = chatService.getChatMessages(id, user.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Send message (REST)", description = "Send a message via REST (fallback for WebSocket)")
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody ChatMessageRequest request) {

        MessageResponse response = chatService.sendMessage(id, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/messages/read")
    @Operation(summary = "Mark as read", description = "Mark all messages in chat as read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {

        chatService.markMessagesAsRead(id, user.getId());
        return ResponseEntity.ok().build();
    }
}
