package kg.freelance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.freelance.dto.request.ReviewRequest;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.dto.response.ReviewResponse;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Review management")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/orders/{orderId}")
    @Operation(summary = "Create review", description = "Leave a review for completed order")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long orderId,
            @Valid @RequestBody ReviewRequest request) {

        ReviewResponse response = reviewService.createReview(user.getId(), orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get review", description = "Get review by ID")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable Long id) {
        ReviewResponse response = reviewService.getReviewById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get review by order", description = "Get review for specific order")
    public ResponseEntity<ReviewResponse> getReviewByOrder(@PathVariable Long orderId) {
        ReviewResponse response = reviewService.getReviewByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/executors/{executorId}")
    @Operation(summary = "Get executor reviews", description = "Get all reviews for an executor")
    public ResponseEntity<PageResponse<ReviewResponse>> getExecutorReviews(
            @PathVariable Long executorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ReviewResponse> response = reviewService.getExecutorReviews(executorId, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update review", description = "Update your review")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ReviewResponse> updateReview(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {

        ReviewResponse response = reviewService.updateReview(user.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete review", description = "Delete your review")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {

        reviewService.deleteReview(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
