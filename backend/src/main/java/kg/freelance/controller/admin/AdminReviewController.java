package kg.freelance.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kg.freelance.dto.response.AdminReviewResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@Tag(name = "Admin - Reviews", description = "Review moderation for admins")
@SecurityRequirement(name = "bearerAuth")
public class AdminReviewController {

    private final AdminService adminService;

    @GetMapping
    @Operation(summary = "Get all reviews", description = "Get all reviews with filters")
    public ResponseEntity<PageResponse<AdminReviewResponse>> getAllReviews(
            @RequestParam(required = false) Boolean moderated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<AdminReviewResponse> response = adminService.getAllReviews(moderated, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending reviews", description = "Get reviews pending moderation")
    public ResponseEntity<PageResponse<AdminReviewResponse>> getPendingReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<AdminReviewResponse> response = adminService.getPendingReviews(pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve review", description = "Approve a review")
    public ResponseEntity<Void> approveReview(@PathVariable Long id) {
        adminService.approveReview(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject review", description = "Reject a review")
    public ResponseEntity<Void> rejectReview(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {

        adminService.rejectReview(id, reason);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete review", description = "Delete a review")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        adminService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
