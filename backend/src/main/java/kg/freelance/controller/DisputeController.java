package kg.freelance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.freelance.dto.request.DisputeEvidenceRequest;
import kg.freelance.dto.request.OpenDisputeRequest;
import kg.freelance.dto.response.DisputeEvidenceResponse;
import kg.freelance.dto.response.DisputeResponse;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/disputes")
@RequiredArgsConstructor
@Tag(name = "Disputes", description = "Dispute management")
@SecurityRequirement(name = "bearerAuth")
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping("/orders/{orderId}")
    @Operation(summary = "Open dispute", description = "Open a dispute for an order")
    public ResponseEntity<DisputeResponse> openDispute(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long orderId,
            @Valid @RequestBody OpenDisputeRequest request) {
        DisputeResponse response = disputeService.openDispute(user.getId(), orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get dispute by order ID")
    public ResponseEntity<DisputeResponse> getDisputeByOrder(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long orderId) {
        DisputeResponse response = disputeService.getDisputeByOrderId(orderId, user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dispute by ID")
    public ResponseEntity<DisputeResponse> getDispute(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        DisputeResponse response = disputeService.getDisputeById(id, user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/evidence")
    @Operation(summary = "Add evidence to dispute")
    public ResponseEntity<DisputeEvidenceResponse> addEvidence(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody DisputeEvidenceRequest request) {
        DisputeEvidenceResponse response = disputeService.addEvidence(id, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/evidence")
    @Operation(summary = "Get dispute evidence")
    public ResponseEntity<List<DisputeEvidenceResponse>> getEvidence(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        List<DisputeEvidenceResponse> response = disputeService.getEvidence(id, user.getId());
        return ResponseEntity.ok(response);
    }
}
