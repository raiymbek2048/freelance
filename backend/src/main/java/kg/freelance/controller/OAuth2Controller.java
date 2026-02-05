package kg.freelance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kg.freelance.dto.response.AuthResponse;
import kg.freelance.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OAuth2", description = "OAuth2 authentication endpoints")
public class OAuth2Controller {

    private final OAuth2Service oAuth2Service;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/google/url")
    @Operation(summary = "Get Google OAuth URL")
    public ResponseEntity<Map<String, String>> getGoogleAuthUrl() {
        String url = oAuth2Service.getGoogleAuthUrl();
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/callback/google")
    @Operation(summary = "Google OAuth callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "error", required = false) String error) {

        if (error != null) {
            log.error("Google OAuth error: {}", error);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/login?error=oauth_failed"))
                    .build();
        }

        try {
            AuthResponse authResponse = oAuth2Service.processGoogleCallback(code);

            // Redirect to frontend with tokens
            String redirectUrl = String.format(
                    "%s/oauth/callback?accessToken=%s&refreshToken=%s",
                    frontendUrl,
                    authResponse.getAccessToken(),
                    authResponse.getRefreshToken()
            );

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();

        } catch (Exception e) {
            log.error("Failed to process Google OAuth callback", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/login?error=oauth_failed"))
                    .build();
        }
    }

    @PostMapping("/google/token")
    @Operation(summary = "Exchange Google auth code for tokens (for mobile apps)")
    public ResponseEntity<AuthResponse> exchangeGoogleToken(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String idToken = request.get("idToken");

        if (idToken != null) {
            // Mobile flow - verify ID token directly
            AuthResponse response = oAuth2Service.processGoogleIdToken(idToken);
            return ResponseEntity.ok(response);
        } else if (code != null) {
            // Web flow - exchange code
            AuthResponse response = oAuth2Service.processGoogleCallback(code);
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().build();
    }
}
