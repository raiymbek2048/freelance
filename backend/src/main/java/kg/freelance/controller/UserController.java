package kg.freelance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.freelance.dto.request.PasswordChangeRequest;
import kg.freelance.dto.request.UserUpdateRequest;
import kg.freelance.dto.response.UserResponse;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get authenticated user's profile")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal user) {
        UserResponse response = userService.getCurrentUser(user.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user", description = "Update authenticated user's profile")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateUser(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change password", description = "Change authenticated user's password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody PasswordChangeRequest request) {
        userService.updatePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Get user's public profile by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getCurrentUser(id);
        return ResponseEntity.ok(response);
    }
}
