package kg.freelance.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.freelance.dto.request.ResolveDisputeRequest;
import kg.freelance.dto.response.DisputeResponse;
import kg.freelance.dto.response.MessageResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/disputes")
@RequiredArgsConstructor
@Tag(name = "Admin - Disputes", description = "Dispute management for admins")
@SecurityRequirement(name = "bearerAuth")
public class AdminDisputeController {

    private final DisputeService disputeService;

    @GetMapping
    @Operation(summary = "Get all disputes")
    public ResponseEntity<PageResponse<DisputeResponse>> getAllDisputes(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(disputeService.getAllDisputes(status, pageable));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active disputes")
    public ResponseEntity<PageResponse<DisputeResponse>> getActiveDisputes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(disputeService.getActiveDisputes(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dispute detail")
    public ResponseEntity<DisputeResponse> getDispute(@PathVariable Long id) {
        return ResponseEntity.ok(disputeService.getDisputeForAdmin(id));
    }

    @PutMapping("/{id}/take")
    @Operation(summary = "Take dispute for review")
    public ResponseEntity<DisputeResponse> takeDispute(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable Long id) {
        return ResponseEntity.ok(disputeService.takeDispute(id, admin.getId()));
    }

    @PutMapping("/{id}/notes")
    @Operation(summary = "Add admin notes")
    public ResponseEntity<DisputeResponse> addNotes(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable Long id,
            @RequestParam String notes) {
        return ResponseEntity.ok(disputeService.addAdminNotes(id, admin.getId(), notes));
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "Resolve dispute")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable Long id,
            @Valid @RequestBody ResolveDisputeRequest request) {
        return ResponseEntity.ok(disputeService.resolveDispute(id, admin.getId(), request));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get dispute chat messages")
    public ResponseEntity<PageResponse<MessageResponse>> getDisputeMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(disputeService.getDisputeMessages(id, pageable));
    }
}
