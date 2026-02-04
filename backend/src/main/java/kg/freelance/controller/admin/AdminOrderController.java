package kg.freelance.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kg.freelance.dto.response.AdminOrderResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin - Orders", description = "Order management for admins")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {

    private final AdminService adminService;

    @GetMapping
    @Operation(summary = "Get all orders", description = "Get paginated list of all orders")
    public ResponseEntity<PageResponse<AdminOrderResponse>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<AdminOrderResponse> response = adminService.getAllOrders(status, categoryId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/disputes")
    @Operation(summary = "Get disputed orders", description = "Get orders with disputes")
    public ResponseEntity<PageResponse<AdminOrderResponse>> getDisputedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<AdminOrderResponse> response = adminService.getDisputedOrders(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Get order details")
    public ResponseEntity<AdminOrderResponse> getOrderById(@PathVariable Long id) {
        AdminOrderResponse response = adminService.getOrderById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/resolve-dispute")
    @Operation(summary = "Resolve dispute", description = "Resolve an order dispute")
    public ResponseEntity<Void> resolveDispute(
            @PathVariable Long id,
            @RequestParam boolean favorClient,
            @RequestParam(required = false) String resolution) {

        adminService.resolveDispute(id, favorClient, resolution);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order", description = "Delete an order")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        adminService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
