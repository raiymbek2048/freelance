package kg.freelance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.freelance.dto.request.VerificationSubmitRequest;
import kg.freelance.dto.response.VerificationResponse;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.ExecutorVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
@Tag(name = "Verification", description = "Executor verification endpoints")
public class ExecutorVerificationController {

    private final ExecutorVerificationService verificationService;

    @GetMapping("/status")
    @Operation(summary = "Get my verification status", description = "Get current user's verification status")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<VerificationResponse> getMyStatus(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(verificationService.getMyStatus(user.getId()));
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit verification request", description = "Submit passport and selfie for verification")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<VerificationResponse> submitVerification(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody VerificationSubmitRequest request) {
        return ResponseEntity.ok(verificationService.submitVerification(user.getId(), request));
    }
}
