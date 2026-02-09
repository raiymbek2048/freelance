package kg.freelance.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kg.freelance.dto.response.AdminStatsResponse;
import kg.freelance.dto.response.AnalyticsResponse;
import kg.freelance.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    @GetMapping("/analytics")
    @Operation(summary = "Get detailed analytics", description = "Get detailed platform analytics with time-series data")
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        AnalyticsResponse response = adminService.getAnalytics();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export analytics as CSV", description = "Download platform analytics data as a CSV file")
    public ResponseEntity<byte[]> exportCsv() {
        byte[] csvData = adminService.exportAnalyticsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
