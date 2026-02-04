package kg.freelance.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kg.freelance.dto.response.AdminVerificationResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.ExecutorVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/verifications")
@RequiredArgsConstructor
@Tag(name = "Admin - Verifications", description = "Admin verification management")
@SecurityRequirement(name = "bearerAuth")
public class AdminVerificationController {

    private final ExecutorVerificationService verificationService;

    @GetMapping
    @Operation(summary = "Get all verifications", description = "Get paginated list of all verification requests")
    public ResponseEntity<PageResponse<AdminVerificationResponse>> getAllVerifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(verificationService.getAllVerifications(pageable));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending verifications", description = "Get paginated list of pending verification requests")
    public ResponseEntity<PageResponse<AdminVerificationResponse>> getPendingVerifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(verificationService.getPendingVerifications(pageable));
    }

    @GetMapping("/count")
    @Operation(summary = "Get pending count", description = "Get count of pending verification requests")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        return ResponseEntity.ok(Map.of("pending", verificationService.countPending()));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get verification details", description = "Get verification request details by user ID")
    public ResponseEntity<AdminVerificationResponse> getVerificationDetails(
            @PathVariable Long userId) {
        return ResponseEntity.ok(verificationService.getVerificationDetails(userId));
    }

    @PutMapping("/{userId}/approve")
    @Operation(summary = "Approve verification", description = "Approve user's verification request")
    public ResponseEntity<Void> approveVerification(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable Long userId) {
        verificationService.approveVerification(userId, admin.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}/reject")
    @Operation(summary = "Reject verification", description = "Reject user's verification request")
    public ResponseEntity<Void> rejectVerification(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable Long userId,
            @RequestParam(required = false) String reason) {
        verificationService.rejectVerification(userId, admin.getId(), reason);
        return ResponseEntity.ok().build();
    }
}
