package kg.freelance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kg.freelance.dto.response.NotificationResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.dto.response.UnreadCountResponse;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notification management")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final InAppNotificationService inAppNotificationService;

    @GetMapping
    @Operation(summary = "Get notifications", description = "Get paginated list of user notifications")
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        PageResponse<NotificationResponse> response = inAppNotificationService.getNotifications(user.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count", description = "Get count of unread notifications")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal user) {

        long count = inAppNotificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark as read", description = "Mark a single notification as read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {

        inAppNotificationService.markAsRead(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all as read", description = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal user) {

        inAppNotificationService.markAllAsRead(user.getId());
        return ResponseEntity.noContent().build();
    }
}
