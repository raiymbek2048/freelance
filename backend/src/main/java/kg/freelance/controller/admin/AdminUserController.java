package kg.freelance.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kg.freelance.dto.response.AdminUserResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users", description = "User management for admins")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminService adminService;

    @GetMapping
    @Operation(summary = "Get all users", description = "Get paginated list of all users")
    public ResponseEntity<PageResponse<AdminUserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) UserRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<AdminUserResponse> response = adminService.getAllUsers(search, active, role, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Get user details")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable Long id) {
        AdminUserResponse response = adminService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/block")
    @Operation(summary = "Block user", description = "Block a user account")
    public ResponseEntity<Void> blockUser(@PathVariable Long id) {
        adminService.blockUser(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/unblock")
    @Operation(summary = "Unblock user", description = "Unblock a user account")
    public ResponseEntity<Void> unblockUser(@PathVariable Long id) {
        adminService.unblockUser(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Change user role", description = "Change user's role")
    public ResponseEntity<Void> changeRole(
            @PathVariable Long id,
            @RequestParam UserRole role) {

        adminService.changeUserRole(id, role);
        return ResponseEntity.ok().build();
    }
}
