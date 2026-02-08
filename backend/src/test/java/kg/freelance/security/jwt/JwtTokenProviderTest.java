package kg.freelance.security.jwt;

import kg.freelance.entity.enums.UserRole;
import kg.freelance.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private UserPrincipal testUser;

    // Must be at least 64 bytes for HS512
    private static final String TEST_SECRET = "ThisIsAVeryLongSecretKeyForTestingPurposesOnlyItMustBeAtLeast64BytesLong!!";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", 86400000L); // 24 hours

        testUser = UserPrincipal.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .role(UserRole.USER)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("Generate Access Token")
    class GenerateAccessTokenTests {

        @Test
        @DisplayName("Should generate a valid access token")
        void shouldGenerateValidAccessToken() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should embed user ID in token")
        void shouldEmbedUserIdInToken() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            assertThat(userId).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should embed email in token")
        void shouldEmbedEmailInToken() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            String email = jwtTokenProvider.getEmailFromToken(token);
            assertThat(email).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should embed role in token")
        void shouldEmbedRoleInToken() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            UserRole role = jwtTokenProvider.getRoleFromToken(token);
            assertThat(role).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("Should embed ADMIN role for admin user")
        void shouldEmbedAdminRole() {
            UserPrincipal admin = UserPrincipal.builder()
                    .id(2L).email("admin@example.com").fullName("Admin")
                    .role(UserRole.ADMIN).active(true).build();

            String token = jwtTokenProvider.generateAccessToken(admin);

            assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo(UserRole.ADMIN);
        }
    }

    @Nested
    @DisplayName("Generate Refresh Token")
    class GenerateRefreshTokenTests {

        @Test
        @DisplayName("Should generate a valid refresh token")
        void shouldGenerateValidRefreshToken() {
            String token = jwtTokenProvider.generateRefreshToken(1L);

            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should embed user ID in refresh token")
        void shouldEmbedUserIdInRefreshToken() {
            String token = jwtTokenProvider.generateRefreshToken(42L);

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            assertThat(userId).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("Validate Token")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should return true for valid token")
        void shouldReturnTrueForValidToken() {
            String token = jwtTokenProvider.generateAccessToken(testUser);
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should return false for malformed token")
        void shouldReturnFalseForMalformedToken() {
            assertThat(jwtTokenProvider.validateToken("not.a.valid.token")).isFalse();
        }

        @Test
        @DisplayName("Should return false for empty token")
        void shouldReturnFalseForEmptyToken() {
            assertThat(jwtTokenProvider.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("Should return false for expired token")
        void shouldReturnFalseForExpiredToken() {
            // Set expiration to 0 to generate an already-expired token
            ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 0L);

            String token = jwtTokenProvider.generateAccessToken(testUser);

            assertThat(jwtTokenProvider.validateToken(token)).isFalse();

            // Restore
            ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 3600000L);
        }

        @Test
        @DisplayName("Should throw SignatureException for token signed with different key")
        void shouldThrowForTokenWithDifferentKey() {
            // Generate token with current key
            String token = jwtTokenProvider.generateAccessToken(testUser);

            // Change the secret
            ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret",
                    "ACompletelyDifferentSecretKeyThatIsAlsoAtLeast64BytesLongForHS512Algorithm!!");

            // SignatureException is not caught by validateToken — it propagates
            assertThatThrownBy(() -> jwtTokenProvider.validateToken(token))
                    .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);

            // Restore
            ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        }
    }

    @Nested
    @DisplayName("Expiration Getters")
    class ExpirationGetterTests {

        @Test
        @DisplayName("Should return access token expiration")
        void shouldReturnAccessTokenExpiration() {
            assertThat(jwtTokenProvider.getAccessTokenExpirationMs()).isEqualTo(3600000L);
        }

        @Test
        @DisplayName("Should return refresh token expiration")
        void shouldReturnRefreshTokenExpiration() {
            assertThat(jwtTokenProvider.getRefreshTokenExpirationMs()).isEqualTo(86400000L);
        }
    }
}
