package kg.freelance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.freelance.dto.request.ExecutorProfileRequest;
import kg.freelance.dto.response.*;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.ExecutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/executors")
@RequiredArgsConstructor
@Tag(name = "Executors", description = "Executor profile management")
public class ExecutorController {

    private final ExecutorService executorService;

    @GetMapping
    @Operation(summary = "Get executors list", description = "Get paginated list of executors with filters")
    public ResponseEntity<PageResponse<ExecutorListResponse>> getExecutors(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Boolean availableOnly,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "rating") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Sort sorting = switch (sort) {
            case "orders" -> Sort.by(Sort.Direction.DESC, "completedOrders");
            case "newest" -> Sort.by(Sort.Direction.DESC, "user.createdAt");
            default -> Sort.by(Sort.Direction.DESC, "rating");
        };

        Pageable pageable = PageRequest.of(page, size, sorting);
        PageResponse<ExecutorListResponse> response = executorService.getExecutors(
                categoryId, minRating, availableOnly, search, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get executor by ID", description = "Get executor's full profile")
    public ResponseEntity<ExecutorResponse> getExecutorById(@PathVariable Long id) {
        ExecutorResponse response = executorService.getExecutorById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/profile")
    @Operation(summary = "Create executor profile", description = "Create or update your executor profile")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ExecutorResponse> createProfile(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ExecutorProfileRequest request) {

        ExecutorResponse response = executorService.createOrUpdateProfile(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Update executor profile", description = "Update your executor profile")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ExecutorResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ExecutorProfileRequest request) {

        ExecutorResponse response = executorService.createOrUpdateProfile(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/categories")
    @Operation(summary = "Update categories", description = "Update your work categories")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ExecutorResponse> updateCategories(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody List<Long> categoryIds) {

        ExecutorResponse response = executorService.updateCategories(user.getId(), categoryIds);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/availability")
    @Operation(summary = "Update availability", description = "Set your availability for new orders")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> updateAvailability(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam boolean available) {

        executorService.updateAvailability(user.getId(), available);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/reviews")
    @Operation(summary = "Get executor reviews", description = "Get reviews for an executor")
    public ResponseEntity<PageResponse<ReviewResponse>> getExecutorReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ReviewResponse> response = executorService.getExecutorReviews(id, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/portfolio")
    @Operation(summary = "Get executor portfolio", description = "Get portfolio items for an executor")
    public ResponseEntity<List<PortfolioResponse>> getExecutorPortfolio(@PathVariable Long id) {
        List<PortfolioResponse> response = executorService.getExecutorPortfolio(id);
        return ResponseEntity.ok(response);
    }
}
