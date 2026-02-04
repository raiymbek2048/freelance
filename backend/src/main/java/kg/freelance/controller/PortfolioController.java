package kg.freelance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.freelance.dto.request.PortfolioRequest;
import kg.freelance.dto.response.PortfolioResponse;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Portfolio management")
@SecurityRequirement(name = "bearerAuth")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/me")
    @Operation(summary = "Get my portfolio", description = "Get your portfolio items")
    public ResponseEntity<List<PortfolioResponse>> getMyPortfolio(
            @AuthenticationPrincipal UserPrincipal user) {

        List<PortfolioResponse> response = portfolioService.getMyPortfolio(user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me")
    @Operation(summary = "Add portfolio item", description = "Add a new item to your portfolio")
    public ResponseEntity<PortfolioResponse> addPortfolioItem(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody PortfolioRequest request) {

        PortfolioResponse response = portfolioService.addPortfolioItem(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/me/{id}")
    @Operation(summary = "Update portfolio item", description = "Update a portfolio item")
    public ResponseEntity<PortfolioResponse> updatePortfolioItem(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody PortfolioRequest request) {

        PortfolioResponse response = portfolioService.updatePortfolioItem(user.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/{id}")
    @Operation(summary = "Delete portfolio item", description = "Delete a portfolio item")
    public ResponseEntity<Void> deletePortfolioItem(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {

        portfolioService.deletePortfolioItem(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/reorder")
    @Operation(summary = "Reorder portfolio", description = "Change the order of portfolio items")
    public ResponseEntity<Void> reorderPortfolio(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody List<Long> itemIds) {

        portfolioService.reorderPortfolio(user.getId(), itemIds);
        return ResponseEntity.ok().build();
    }
}
