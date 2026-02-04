package kg.freelance.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kg.freelance.dto.response.AdminStatsResponse;
import kg.freelance.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@Tag(name = "Admin - Statistics", description = "Platform statistics for admins")
@SecurityRequirement(name = "bearerAuth")
public class AdminStatsController {

    private final AdminService adminService;

    @GetMapping("/overview")
    @Operation(summary = "Get overview stats", description = "Get platform overview statistics")
    public ResponseEntity<AdminStatsResponse> getOverviewStats() {
        AdminStatsResponse response = adminService.getOverviewStats();
        return ResponseEntity.ok(response);
    }
}
