package kg.freelance.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kg.freelance.dto.response.AuthResponse;
import kg.freelance.dto.response.UserResponse;
import kg.freelance.entity.User;
import kg.freelance.entity.enums.AuthProvider;
import kg.freelance.entity.enums.ProfileVisibility;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.repository.UserRepository;
import kg.freelance.security.UserPrincipal;
import kg.freelance.security.jwt.JwtTokenProvider;
import kg.freelance.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2ServiceImpl implements OAuth2Service {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getGoogleAuthUrl() {
        String redirectUri = baseUrl + "/api/v1/auth/oauth2/callback/google";

        return GOOGLE_AUTH_URL +
                "?client_id=" + googleClientId +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode("email profile openid", StandardCharsets.UTF_8) +
                "&access_type=offline" +
                "&prompt=consent";
    }

    @Override
    @Transactional
    public AuthResponse processGoogleCallback(String code) {
        // Exchange code for access token
        String accessToken = exchangeCodeForToken(code);

        // Get user info from Google
        GoogleUserInfo userInfo = getGoogleUserInfo(accessToken);

        // Create or update user
        User user = findOrCreateUser(userInfo);

        // Generate JWT tokens
        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse processGoogleIdToken(String idToken) {
        // Decode and verify ID token (simplified - in production use Google's token verification)
        GoogleUserInfo userInfo = decodeIdToken(idToken);

        // Create or update user
        User user = findOrCreateUser(userInfo);

        // Generate JWT tokens
        return generateAuthResponse(user);
    }

    private String exchangeCodeForToken(String code) {
        String redirectUri = baseUrl + "/api/v1/auth/oauth2/callback/google";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("code", code);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GOOGLE_TOKEN_URL, request, String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            return json.get("access_token").asText();

        } catch (Exception e) {
            log.error("Failed to exchange code for token", e);
            throw new RuntimeException("Failed to exchange authorization code", e);
        }
    }

    private GoogleUserInfo getGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    GOOGLE_USER_INFO_URL, HttpMethod.GET, request, String.class);

            JsonNode json = objectMapper.readTree(response.getBody());

            return new GoogleUserInfo(
                    json.get("sub").asText(),
                    json.get("email").asText(),
                    json.has("name") ? json.get("name").asText() : json.get("email").asText(),
                    json.has("picture") ? json.get("picture").asText() : null,
                    json.has("email_verified") && json.get("email_verified").asBoolean()
            );

        } catch (Exception e) {
            log.error("Failed to get user info from Google", e);
            throw new RuntimeException("Failed to get user info from Google", e);
        }
    }

    private GoogleUserInfo decodeIdToken(String idToken) {
        try {
            // ID token has 3 parts: header.payload.signature
            String[] parts = idToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode json = objectMapper.readTree(payload);

            return new GoogleUserInfo(
                    json.get("sub").asText(),
                    json.get("email").asText(),
                    json.has("name") ? json.get("name").asText() : json.get("email").asText(),
                    json.has("picture") ? json.get("picture").asText() : null,
                    json.has("email_verified") && json.get("email_verified").asBoolean()
            );

        } catch (Exception e) {
            log.error("Failed to decode ID token", e);
            throw new RuntimeException("Invalid ID token", e);
        }
    }

    private User findOrCreateUser(GoogleUserInfo userInfo) {
        // Try to find by Google ID first
        User user = userRepository.findByGoogleId(userInfo.googleId()).orElse(null);

        if (user != null) {
            // Update user info if changed
            if (userInfo.pictureUrl() != null && !userInfo.pictureUrl().equals(user.getAvatarUrl())) {
                user.setAvatarUrl(userInfo.pictureUrl());
            }
            return userRepository.save(user);
        }

        // Try to find by email (user might have registered with email/password before)
        user = userRepository.findByEmail(userInfo.email()).orElse(null);

        if (user != null) {
            // Link Google account to existing user
            user.setGoogleId(userInfo.googleId());
            user.setAuthProvider(AuthProvider.GOOGLE);
            if (user.getAvatarUrl() == null && userInfo.pictureUrl() != null) {
                user.setAvatarUrl(userInfo.pictureUrl());
            }
            if (userInfo.emailVerified()) {
                user.setEmailVerified(true);
            }
            return userRepository.save(user);
        }

        // Create new user
        user = User.builder()
                .email(userInfo.email())
                .fullName(userInfo.name())
                .googleId(userInfo.googleId())
                .authProvider(AuthProvider.GOOGLE)
                .avatarUrl(userInfo.pictureUrl())
                .emailVerified(userInfo.emailVerified())
                .phoneVerified(false)
                .executorVerified(false)
                .role(UserRole.USER)
                .profileVisibility(ProfileVisibility.PUBLIC)
                .hideFromExecutorList(false)
                .active(true)
                .build();

        return userRepository.save(user);
    }

    private AuthResponse generateAuthResponse(User user) {
        UserPrincipal userPrincipal = UserPrincipal.fromUser(user);

        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .whatsappLink(user.getWhatsappLink())
                .profileVisibility(user.getProfileVisibility())
                .hideFromExecutorList(user.getHideFromExecutorList())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .executorVerified(user.getExecutorVerified())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .hasExecutorProfile(user.getExecutorProfile() != null)
                .build();

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationMs(),
                userResponse
        );
    }

    private record GoogleUserInfo(
            String googleId,
            String email,
            String name,
            String pictureUrl,
            boolean emailVerified
    ) {}
}
