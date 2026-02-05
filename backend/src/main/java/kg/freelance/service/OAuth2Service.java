package kg.freelance.service;

import kg.freelance.dto.response.AuthResponse;

public interface OAuth2Service {

    /**
     * Get Google OAuth authorization URL
     */
    String getGoogleAuthUrl();

    /**
     * Process Google OAuth callback with authorization code
     */
    AuthResponse processGoogleCallback(String code);

    /**
     * Process Google ID token (for mobile apps)
     */
    AuthResponse processGoogleIdToken(String idToken);
}
