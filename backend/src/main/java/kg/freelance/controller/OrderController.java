package kg.freelance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.freelance.dto.request.*;
import kg.freelance.dto.response.*;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.OrderService;
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
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get public orders", description = "Get paginated list of public orders")
    public ResponseEntity<PageResponse<OrderListResponse>> getPublicOrders(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal budgetMin,
            @RequestParam(required = false) BigDecimal budgetMax,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Sort sorting = switch (sort) {
            case "budget" -> Sort.by(Sort.Direction.DESC, "budget_max");
            case "deadline" -> Sort.by(Sort.Direction.ASC, "deadline");
            default -> Sort.by(Sort.Direction.DESC, "created_at");
        };

        Long userId = user != null ? user.getId() : null;
        Pageable pageable = PageRequest.of(page, size, sorting);
        PageResponse<OrderListResponse> response = orderService.getPublicOrders(
                categoryId, budgetMin, budgetMax, search, location, userId, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Get order details")
    public ResponseEntity<OrderDetailResponse> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user) {

        Long userId = user != null ? user.getId() : null;
        OrderDetailResponse response = orderService.getOrderById(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create order", description = "Create a new order")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OrderDetailResponse> createOrder(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody OrderCreateRequest request) {

        OrderDetailResponse response = orderService.createOrder(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update order", description = "Update order details")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OrderDetailResponse> updateOrder(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody OrderUpdateRequest request) {

        OrderDetailResponse response = orderService.updateOrder(user.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order", description = "Delete an order")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteOrder(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {

        orderService.deleteOrder(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/select-executor")
    @Operation(summary = "Select executor", description = "Select an executor for the order")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> selectExecutor(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody SelectExecutorRequest request) {

        orderService.selectExecutor(user.getId(), id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/submit-for-review")
    @Operation(summary = "Submit for review", description = "Submit work for client review (executor)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> submitForReview(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {

        orderService.submitForReview(user.getId(), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve work", description = "Approve completed work (client)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> approveWork(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {

        orderService.approveWork(user.getId(), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/request-revision")
    @Operation(summary = "Request revision", description = "Request work revision (client)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> requestRevision(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {

        orderService.requestRevision(user.getId(), id, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/dispute")
    @Operation(summary = "Open dispute", description = "Open a dispute for the order")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<DisputeResponse> openDispute(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody OpenDisputeRequest request) {

        orderService.openDispute(user.getId(), id, request.getReason());
        return ResponseEntity.ok().build();
    }

    // My orders

    @GetMapping("/my/as-client")
    @Operation(summary = "Get my orders as client", description = "Get orders where I am the client")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PageResponse<OrderListResponse>> getMyOrdersAsClient(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<OrderListResponse> response = orderService.getMyOrdersAsClient(user.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my/as-executor")
    @Operation(summary = "Get my orders as executor", description = "Get orders where I am the executor")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PageResponse<OrderListResponse>> getMyOrdersAsExecutor(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<OrderListResponse> response = orderService.getMyOrdersAsExecutor(user.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    // Responses

    @GetMapping("/{id}/responses")
    @Operation(summary = "Get order responses", description = "Get responses for an order (client only)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<OrderResponseDto>> getOrderResponses(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {

        List<OrderResponseDto> responses = orderService.getOrderResponses(id, user.getId());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/responses")
    @Operation(summary = "Create response", description = "Respond to an order (executor)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OrderResponseDto> createResponse(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody OrderResponseRequest request) {

        OrderResponseDto response = orderService.createResponse(user.getId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my/responses")
    @Operation(summary = "Get my responses", description = "Get my responses to orders")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PageResponse<OrderResponseDto>> getMyResponses(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<OrderResponseDto> response = orderService.getMyResponses(user.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/responses/{responseId}")
    @Operation(summary = "Update response", description = "Update your response")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OrderResponseDto> updateResponse(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long responseId,
            @Valid @RequestBody OrderResponseRequest request) {

        OrderResponseDto response = orderService.updateResponse(user.getId(), responseId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/responses/{responseId}")
    @Operation(summary = "Delete response", description = "Delete your response")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteResponse(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long responseId) {

        orderService.deleteResponse(user.getId(), responseId);
        return ResponseEntity.noContent().build();
    }
}
