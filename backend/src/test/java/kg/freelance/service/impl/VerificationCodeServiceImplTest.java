package kg.freelance.service.impl;

import kg.freelance.entity.User;
import kg.freelance.entity.VerificationCode;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.entity.enums.VerificationType;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.UserRepository;
import kg.freelance.repository.VerificationCodeRepository;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationCodeService Tests")
class VerificationCodeServiceImplTest {

    @Mock
    private VerificationCodeRepository verificationCodeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private VerificationCodeServiceImpl verificationCodeService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .phone("+996555123456")
                .fullName("Test User")
                .role(UserRole.USER)
                .emailVerified(false)
                .phoneVerified(false)
                .build();
    }

    @Nested
    @DisplayName("Send Verification Code Tests")
    class SendVerificationCodeTests {

        @Test
        @DisplayName("Should send email verification code")
        void shouldSendEmailVerificationCode() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(verificationCodeRepository.countByUserIdAndTypeAndCreatedAtAfter(
                    eq(1L), eq(VerificationType.EMAIL), any(LocalDateTime.class)))
                    .thenReturn(0L);

            // When
            verificationCodeService.sendVerificationCode(1L, VerificationType.EMAIL);

            // Then
            ArgumentCaptor<VerificationCode> codeCaptor = ArgumentCaptor.forClass(VerificationCode.class);
            verify(verificationCodeRepository).save(codeCaptor.capture());
            VerificationCode savedCode = codeCaptor.getValue();
            assertThat(savedCode.getCode()).hasSize(6);
            assertThat(savedCode.getCode()).matches("\\d{6}");
            assertThat(savedCode.getType()).isEqualTo(VerificationType.EMAIL);
            assertThat(savedCode.getUsed()).isFalse();
            assertThat(savedCode.getExpiresAt()).isAfter(LocalDateTime.now());
            verify(emailService).sendEmailVerificationCode(eq("test@example.com"), anyString());
        }

        @Test
        @DisplayName("Should send phone verification code")
        void shouldSendPhoneVerificationCode() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(verificationCodeRepository.countByUserIdAndTypeAndCreatedAtAfter(
                    eq(1L), eq(VerificationType.PHONE), any(LocalDateTime.class)))
                    .thenReturn(0L);

            // When
            verificationCodeService.sendVerificationCode(1L, VerificationType.PHONE);

            // Then
            verify(verificationCodeRepository).save(any(VerificationCode.class));
            // Phone sends SMS (logged in dev mode), no email service call
            verify(emailService, never()).sendEmailVerificationCode(anyString(), anyString());
        }

        @Test
        @DisplayName("Should send password reset code")
        void shouldSendPasswordResetCode() {
            // Given
            user.setEmailVerified(true); // Already verified - should still allow password reset
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(verificationCodeRepository.countByUserIdAndTypeAndCreatedAtAfter(
                    eq(1L), eq(VerificationType.PASSWORD_RESET), any(LocalDateTime.class)))
                    .thenReturn(0L);

            // When
            verificationCodeService.sendVerificationCode(1L, VerificationType.PASSWORD_RESET);

            // Then
            verify(verificationCodeRepository).save(any(VerificationCode.class));
            verify(emailService).sendPasswordResetCode(eq("test@example.com"), anyString());
        }

        @Test
        @DisplayName("Should throw exception when email already verified")
        void shouldThrowExceptionWhenEmailAlreadyVerified() {
            // Given
            user.setEmailVerified(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(verificationCodeRepository.countByUserIdAndTypeAndCreatedAtAfter(
                    eq(1L), eq(VerificationType.EMAIL), any(LocalDateTime.class)))
                    .thenReturn(0L);

            // When/Then
            assertThatThrownBy(() -> verificationCodeService.sendVerificationCode(1L, VerificationType.EMAIL))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Email уже подтверждён");
        }

        @Test
        @DisplayName("Should throw exception when phone already verified")
        void shouldThrowExceptionWhenPhoneAlreadyVerified() {
            // Given
            user.setPhoneVerified(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(verificationCodeRepository.countByUserIdAndTypeAndCreatedAtAfter(
                    eq(1L), eq(VerificationType.PHONE), any(LocalDateTime.class)))
                    .thenReturn(0L);

            // When/Then
            assertThatThrownBy(() -> verificationCodeService.sendVerificationCode(1L, VerificationType.PHONE))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Телефон уже подтверждён");
        }

        @Test
        @DisplayName("Should throw exception when rate limited")
        void shouldThrowExceptionWhenRateLimited() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(verificationCodeRepository.countByUserIdAndTypeAndCreatedAtAfter(
                    eq(1L), eq(VerificationType.EMAIL), any(LocalDateTime.class)))
                    .thenReturn(5L); // MAX_REQUESTS_PER_HOUR = 5

            // When/Then
            assertThatThrownBy(() -> verificationCodeService.sendVerificationCode(1L, VerificationType.EMAIL))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Слишком много запросов. Попробуйте позже.");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> verificationCodeService.sendVerificationCode(999L, VerificationType.EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Verify Code Tests")
    class VerifyCodeTests {

        @Test
        @DisplayName("Should verify email code successfully")
        void shouldVerifyEmailCodeSuccessfully() {
            // Given
            VerificationCode code = VerificationCode.builder()
                    .id(1L)
                    .user(user)
                    .code("123456")
                    .type(VerificationType.EMAIL)
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .used(false)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(verificationCodeRepository.findByUserIdAndCodeAndTypeAndUsedFalseAndExpiresAtAfter(
                    eq(1L), eq("123456"), eq(VerificationType.EMAIL), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(code));

            // When
            boolean result = verificationCodeService.verifyCode(1L, "123456", VerificationType.EMAIL);

            // Then
            assertThat(result).isTrue();
            assertThat(code.getUsed()).isTrue();
            assertThat(user.getEmailVerified()).isTrue();
            verify(verificationCodeRepository).save(code);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should verify phone code successfully")
        void shouldVerifyPhoneCodeSuccessfully() {
            // Given
            VerificationCode code = VerificationCode.builder()
                    .id(2L)
                    .user(user)
                    .code("654321")
                    .type(VerificationType.PHONE)
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .used(false)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(verificationCodeRepository.findByUserIdAndCodeAndTypeAndUsedFalseAndExpiresAtAfter(
                    eq(1L), eq("654321"), eq(VerificationType.PHONE), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(code));

            // When
            boolean result = verificationCodeService.verifyCode(1L, "654321", VerificationType.PHONE);

            // Then
            assertThat(result).isTrue();
            assertThat(user.getPhoneVerified()).isTrue();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should verify password reset code without updating user status")
        void shouldVerifyPasswordResetCodeWithoutUpdatingUserStatus() {
            // Given
            VerificationCode code = VerificationCode.builder()
                    .id(3L)
                    .user(user)
                    .code("111111")
                    .type(VerificationType.PASSWORD_RESET)
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .used(false)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(verificationCodeRepository.findByUserIdAndCodeAndTypeAndUsedFalseAndExpiresAtAfter(
                    eq(1L), eq("111111"), eq(VerificationType.PASSWORD_RESET), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(code));

            // When
            boolean result = verificationCodeService.verifyCode(1L, "111111", VerificationType.PASSWORD_RESET);

            // Then
            assertThat(result).isTrue();
            assertThat(code.getUsed()).isTrue();
            verify(verificationCodeRepository).save(code);
            verify(userRepository, never()).save(user); // Should NOT update user for PASSWORD_RESET
        }

        @Test
        @DisplayName("Should return false for invalid code")
        void shouldReturnFalseForInvalidCode() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(verificationCodeRepository.findByUserIdAndCodeAndTypeAndUsedFalseAndExpiresAtAfter(
                    eq(1L), eq("000000"), eq(VerificationType.EMAIL), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            // When
            boolean result = verificationCodeService.verifyCode(1L, "000000", VerificationType.EMAIL);

            // Then
            assertThat(result).isFalse();
            verify(verificationCodeRepository, never()).save(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found for verification")
        void shouldThrowExceptionWhenUserNotFoundForVerification() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> verificationCodeService.verifyCode(999L, "123456", VerificationType.EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Can Request New Code Tests")
    class CanRequestNewCodeTests {

        @Test
        @DisplayName("Should allow when under rate limit")
        void shouldAllowWhenUnderRateLimit() {
            // Given
            when(verificationCodeRepository.countByUserIdAndTypeAndCreatedAtAfter(
                    eq(1L), eq(VerificationType.EMAIL), any(LocalDateTime.class)))
                    .thenReturn(4L);

            // When
            boolean result = verificationCodeService.canRequestNewCode(1L, VerificationType.EMAIL);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should deny when at rate limit")
        void shouldDenyWhenAtRateLimit() {
            // Given
            when(verificationCodeRepository.countByUserIdAndTypeAndCreatedAtAfter(
                    eq(1L), eq(VerificationType.EMAIL), any(LocalDateTime.class)))
                    .thenReturn(5L);

            // When
            boolean result = verificationCodeService.canRequestNewCode(1L, VerificationType.EMAIL);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should deny when over rate limit")
        void shouldDenyWhenOverRateLimit() {
            // Given
            when(verificationCodeRepository.countByUserIdAndTypeAndCreatedAtAfter(
                    eq(1L), eq(VerificationType.EMAIL), any(LocalDateTime.class)))
                    .thenReturn(10L);

            // When
            boolean result = verificationCodeService.canRequestNewCode(1L, VerificationType.EMAIL);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should allow when zero requests")
        void shouldAllowWhenZeroRequests() {
            // Given
            when(verificationCodeRepository.countByUserIdAndTypeAndCreatedAtAfter(
                    eq(1L), eq(VerificationType.EMAIL), any(LocalDateTime.class)))
                    .thenReturn(0L);

            // When
            boolean result = verificationCodeService.canRequestNewCode(1L, VerificationType.EMAIL);

            // Then
            assertThat(result).isTrue();
        }
    }
}
