package kg.freelance.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.freelance.dto.request.GrantSubscriptionRequest;
import kg.freelance.dto.request.SubscriptionSettingsRequest;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.dto.response.SubscriptionSettingsResponse;
import kg.freelance.dto.response.UserSubscriptionResponse;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/subscription")
@RequiredArgsConstructor
@Tag(name = "Admin - Subscriptions", description = "Admin subscription management")
@SecurityRequirement(name = "bearerAuth")
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/settings")
    @Operation(summary = "Get subscription settings", description = "Get current subscription settings")
    public ResponseEntity<SubscriptionSettingsResponse> getSettings() {
        return ResponseEntity.ok(subscriptionService.getSettings());
    }

    @PutMapping("/settings")
    @Operation(summary = "Update subscription settings", description = "Update subscription settings")
    public ResponseEntity<SubscriptionSettingsResponse> updateSettings(
            @AuthenticationPrincipal UserPrincipal admin,
            @Valid @RequestBody SubscriptionSettingsRequest request) {
        return ResponseEntity.ok(subscriptionService.updateSettings(request, admin.getId()));
    }

    @GetMapping("/users")
    @Operation(summary = "Get all subscriptions", description = "Get paginated list of all user subscriptions")
    public ResponseEntity<PageResponse<UserSubscriptionResponse>> getAllSubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserSubscriptionResponse> result = subscriptionService.getAllSubscriptions(pageable);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    @PostMapping("/users/{userId}/grant")
    @Operation(summary = "Grant subscription", description = "Grant subscription to a user")
    public ResponseEntity<UserSubscriptionResponse> grantSubscription(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable Long userId,
            @Valid @RequestBody GrantSubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.grantSubscription(userId, request, admin.getId()));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Revoke subscription", description = "Revoke user's active subscription")
    public ResponseEntity<Void> revokeSubscription(@PathVariable Long userId) {
        subscriptionService.revokeSubscription(userId);
        return ResponseEntity.ok().build();
    }
}
