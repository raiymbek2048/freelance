package kg.freelance.service.impl;

import kg.freelance.dto.request.LoginRequest;
import kg.freelance.dto.request.RefreshTokenRequest;
import kg.freelance.dto.request.RegisterRequest;
import kg.freelance.dto.response.AuthResponse;
import kg.freelance.entity.ExecutorProfile;
import kg.freelance.entity.User;
import kg.freelance.entity.enums.ProfileVisibility;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.UnauthorizedException;
import kg.freelance.repository.ExecutorProfileRepository;
import kg.freelance.repository.UserRepository;
import kg.freelance.security.UserPrincipal;
import kg.freelance.security.jwt.JwtTokenProvider;
import kg.freelance.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExecutorProfileRepository executorProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private ExecutorProfile testExecutorProfile;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .fullName("Test User")
                .phone("+996700123456")
                .role(UserRole.USER)
                .profileVisibility(ProfileVisibility.PUBLIC)
                .hideFromExecutorList(false)
                .emailVerified(false)
                .phoneVerified(false)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        testExecutorProfile = new ExecutorProfile();
        testExecutorProfile.setId(1L);
        testExecutorProfile.setUser(testUser);
        testExecutorProfile.setTotalOrders(0);
        testExecutorProfile.setCompletedOrders(0);
        testExecutorProfile.setRating(BigDecimal.ZERO);
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUserSuccessfully() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@example.com");
            request.setPassword("password123");
            request.setFullName("New User");
            request.setPhone("+996700111222");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });
            when(executorProfileRepository.save(any(ExecutorProfile.class))).thenAnswer(invocation -> {
                ExecutorProfile profile = invocation.getArgument(0);
                profile.setId(1L);
                return profile;
            });
            when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("accessToken");
            when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refreshToken");
            when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);
            doNothing().when(emailService).sendWelcomeEmail(any(User.class));

            // When
            AuthResponse response = authService.register(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
            assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo("new@example.com");

            verify(userRepository).save(any(User.class));
            verify(executorProfileRepository).save(any(ExecutorProfile.class));
            verify(emailService).sendWelcomeEmail(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setEmail("existing@example.com");
            request.setPassword("password123");
            request.setFullName("Test User");

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Email is already registered");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when phone already exists")
        void shouldThrowExceptionWhenPhoneExists() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@example.com");
            request.setPassword("password123");
            request.setFullName("Test User");
            request.setPhone("+996700123456");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone("+996700123456")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Phone number is already registered");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should normalize email to lowercase")
        void shouldNormalizeEmailToLowercase() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setEmail("TEST@EXAMPLE.COM");
            request.setPassword("password123");
            request.setFullName("Test User");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });
            when(executorProfileRepository.save(any(ExecutorProfile.class))).thenReturn(testExecutorProfile);
            when(jwtTokenProvider.generateAccessToken(any())).thenReturn("token");
            when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh");
            when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);
            doNothing().when(emailService).sendWelcomeEmail(any(User.class));

            // When
            authService.register(request);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getEmail()).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully")
        void shouldLoginUserSuccessfully() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("password123");

            testUser.setExecutorProfile(testExecutorProfile);
            UserPrincipal userPrincipal = UserPrincipal.fromUser(testUser);
            Authentication authentication = mock(Authentication.class);

            when(authentication.getPrincipal()).thenReturn(userPrincipal);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("accessToken");
            when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refreshToken");
            when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

            // When
            AuthResponse response = authService.login(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
            assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
            assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should throw exception for invalid credentials")
        void shouldThrowExceptionForInvalidCredentials() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("wrongPassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("Should normalize email to lowercase on login")
        void shouldNormalizeEmailOnLogin() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setEmail("TEST@EXAMPLE.COM");
            request.setPassword("password123");

            testUser.setExecutorProfile(testExecutorProfile);
            UserPrincipal userPrincipal = UserPrincipal.fromUser(testUser);
            Authentication authentication = mock(Authentication.class);

            when(authentication.getPrincipal()).thenReturn(userPrincipal);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(jwtTokenProvider.generateAccessToken(any())).thenReturn("token");
            when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh");
            when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

            // When
            authService.login(request);

            // Then
            ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getPrincipal()).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("validRefreshToken");

            testUser.setExecutorProfile(testExecutorProfile);

            when(jwtTokenProvider.validateToken("validRefreshToken")).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken("validRefreshToken")).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("newAccessToken");
            when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("newRefreshToken");
            when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

            // When
            AuthResponse response = authService.refreshToken(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
            assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token")
        void shouldThrowExceptionForInvalidRefreshToken() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("invalidToken");

            when(jwtTokenProvider.validateToken("invalidToken")).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid or expired refresh token");
        }

        @Test
        @DisplayName("Should throw exception for disabled user account")
        void shouldThrowExceptionForDisabledUser() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("validToken");

            testUser.setActive(false);

            when(jwtTokenProvider.validateToken("validToken")).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken("validToken")).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When/Then
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("User account is disabled");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("validToken");

            when(jwtTokenProvider.validateToken("validToken")).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken("validToken")).thenReturn(999L);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("User not found");
        }
    }
}
