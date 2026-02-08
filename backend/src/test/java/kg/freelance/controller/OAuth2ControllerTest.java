package kg.freelance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kg.freelance.dto.response.AuthResponse;
import kg.freelance.exception.GlobalExceptionHandler;
import kg.freelance.service.OAuth2Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2Controller Tests")
class OAuth2ControllerTest {

    @Mock
    private OAuth2Service oAuth2Service;

    @InjectMocks
    private OAuth2Controller controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "frontendUrl", "http://localhost:3000");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("GET /api/v1/auth/oauth2/google/url")
    class GetGoogleAuthUrlTests {

        @Test
        @DisplayName("Should return Google auth URL")
        void shouldReturnGoogleAuthUrl() throws Exception {
            when(oAuth2Service.getGoogleAuthUrl()).thenReturn("https://accounts.google.com/o/oauth2/auth?client_id=xxx");

            mockMvc.perform(get("/api/v1/auth/oauth2/google/url"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://accounts.google.com/o/oauth2/auth?client_id=xxx"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/oauth2/callback/google")
    class GoogleCallbackTests {

        @Test
        @DisplayName("Should redirect with tokens on success")
        void shouldRedirectWithTokensOnSuccess() throws Exception {
            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .tokenType("Bearer")
                    .build();

            when(oAuth2Service.processGoogleCallback("auth-code")).thenReturn(authResponse);

            mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                            .param("code", "auth-code"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location",
                            "http://localhost:3000/oauth/callback?accessToken=access-token&refreshToken=refresh-token"));
        }

        @Test
        @DisplayName("Should redirect to login on error param")
        void shouldRedirectToLoginOnError() throws Exception {
            mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                            .param("code", "anything")
                            .param("error", "access_denied"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location",
                            "http://localhost:3000/login?error=oauth_failed"));
        }

        @Test
        @DisplayName("Should redirect to login on exception")
        void shouldRedirectToLoginOnException() throws Exception {
            when(oAuth2Service.processGoogleCallback("bad-code"))
                    .thenThrow(new RuntimeException("Token exchange failed"));

            mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                            .param("code", "bad-code"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location",
                            "http://localhost:3000/login?error=oauth_failed"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/oauth2/google/token")
    class ExchangeGoogleTokenTests {

        @Test
        @DisplayName("Should exchange auth code for tokens")
        void shouldExchangeAuthCodeForTokens() throws Exception {
            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .tokenType("Bearer")
                    .build();

            when(oAuth2Service.processGoogleCallback("auth-code")).thenReturn(authResponse);

            mockMvc.perform(post("/api/v1/auth/oauth2/google/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("code", "auth-code"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"));
        }

        @Test
        @DisplayName("Should exchange ID token for mobile flow")
        void shouldExchangeIdTokenForMobile() throws Exception {
            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken("mobile-token")
                    .refreshToken("mobile-refresh")
                    .tokenType("Bearer")
                    .build();

            when(oAuth2Service.processGoogleIdToken("google-id-token")).thenReturn(authResponse);

            mockMvc.perform(post("/api/v1/auth/oauth2/google/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("idToken", "google-id-token"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("mobile-token"));
        }

        @Test
        @DisplayName("Should return 400 when no code or idToken provided")
        void shouldReturn400WhenNoCodeOrIdToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/oauth2/google/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("other", "value"))))
                    .andExpect(status().isBadRequest());
        }
    }
}
