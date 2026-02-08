package kg.freelance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kg.freelance.dto.request.LoginRequest;
import kg.freelance.dto.request.RefreshTokenRequest;
import kg.freelance.dto.request.RegisterRequest;
import kg.freelance.dto.response.AuthResponse;
import kg.freelance.dto.response.UserResponse;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.GlobalExceptionHandler;
import kg.freelance.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    private AuthResponse buildAuthResponse() {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .build();
        return AuthResponse.of("access-token", "refresh-token", 3600000L, userResponse);
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUserSuccessfully() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("test@example.com");
            request.setPassword("Password123!");
            request.setFullName("Test User");

            when(authService.register(any(RegisterRequest.class))).thenReturn(buildAuthResponse());

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.user.email").value("test@example.com"));
        }

        @Test
        @DisplayName("Should return 400 when email already exists")
        void shouldReturn400WhenEmailAlreadyExists() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("existing@example.com");
            request.setPassword("Password123!");
            request.setFullName("Test User");

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new BadRequestException("Email already registered"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Email already registered"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully")
        void shouldLoginSuccessfully() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("Password123!");

            when(authService.login(any(LoginRequest.class))).thenReturn(buildAuthResponse());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.user.email").value("test@example.com"));
        }

        @Test
        @DisplayName("Should return 401 for bad credentials")
        void shouldReturn401ForBadCredentials() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("wrong");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Invalid email or password"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-refresh-token");

            when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(buildAuthResponse());

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogoutSuccessfully() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isOk());
        }
    }
}
