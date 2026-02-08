package kg.freelance.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserService userService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilterTests {

        @Test
        @DisplayName("Should skip filter for GET file requests")
        void shouldSkipFilterForGetFileRequests() {
            request.setServletPath("/api/v1/files/general/1/image.jpg");
            request.setMethod("GET");

            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("Should not skip filter for POST file requests")
        void shouldNotSkipFilterForPostFileRequests() {
            request.setServletPath("/api/v1/files/upload");
            request.setMethod("POST");

            assertThat(filter.shouldNotFilter(request)).isFalse();
        }

        @Test
        @DisplayName("Should not skip filter for other paths")
        void shouldNotSkipFilterForOtherPaths() {
            request.setServletPath("/api/v1/orders");
            request.setMethod("GET");

            assertThat(filter.shouldNotFilter(request)).isFalse();
        }
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternalTests {

        @Test
        @DisplayName("Should set authentication for valid token")
        void shouldSetAuthenticationForValidToken() throws ServletException, IOException {
            String token = "valid-jwt-token";
            request.addHeader("Authorization", "Bearer " + token);

            UserPrincipal userPrincipal = UserPrincipal.builder()
                    .id(1L).email("test@example.com").fullName("Test")
                    .role(UserRole.USER).active(true).build();

            when(jwtTokenProvider.validateToken(token)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(1L);
            when(userService.loadUserById(1L)).thenReturn(userPrincipal);

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isEqualTo(userPrincipal);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should not set authentication when no Authorization header")
        void shouldNotSetAuthWhenNoHeader() throws ServletException, IOException {
            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should not set authentication for invalid token")
        void shouldNotSetAuthForInvalidToken() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer invalid-token");

            when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should not set authentication for non-Bearer header")
        void shouldNotSetAuthForNonBearerHeader() throws ServletException, IOException {
            request.addHeader("Authorization", "Basic abc123");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should not set authentication for inactive user")
        void shouldNotSetAuthForInactiveUser() throws ServletException, IOException {
            String token = "valid-token";
            request.addHeader("Authorization", "Bearer " + token);

            UserPrincipal inactiveUser = UserPrincipal.builder()
                    .id(1L).email("blocked@example.com").fullName("Blocked")
                    .role(UserRole.USER).active(false).build();

            when(jwtTokenProvider.validateToken(token)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(1L);
            when(userService.loadUserById(1L)).thenReturn(inactiveUser);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should continue filter chain even on exception")
        void shouldContinueFilterChainOnException() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer some-token");

            when(jwtTokenProvider.validateToken("some-token")).thenThrow(new RuntimeException("DB error"));

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should not set authentication when user is null")
        void shouldNotSetAuthWhenUserIsNull() throws ServletException, IOException {
            String token = "valid-token";
            request.addHeader("Authorization", "Bearer " + token);

            when(jwtTokenProvider.validateToken(token)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(999L);
            when(userService.loadUserById(999L)).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }
    }
}
