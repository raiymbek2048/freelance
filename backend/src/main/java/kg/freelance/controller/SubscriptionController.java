package kg.freelance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kg.freelance.dto.response.AnnouncementResponse;
import kg.freelance.dto.response.MySubscriptionResponse;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "User subscription endpoints")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/announcement")
    @Operation(summary = "Get announcement", description = "Get current announcement for the main page")
    public ResponseEntity<AnnouncementResponse> getAnnouncement() {
        return ResponseEntity.ok(subscriptionService.getAnnouncement());
    }

    @GetMapping("/my")
    @Operation(summary = "Get my subscription", description = "Get current user's subscription status")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MySubscriptionResponse> getMySubscription(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(subscriptionService.getMySubscription(user.getId()));
    }

    @PostMapping("/start-trial")
    @Operation(summary = "Start trial", description = "Start free trial period")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MySubscriptionResponse> startTrial(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(subscriptionService.startTrial(user.getId()));
    }
}
