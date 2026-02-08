package kg.freelance.service;

import kg.freelance.dto.request.VerificationSubmitRequest;
import kg.freelance.dto.response.AdminVerificationResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.dto.response.VerificationResponse;
import kg.freelance.entity.ExecutorProfile;
import kg.freelance.entity.ExecutorVerification;
import kg.freelance.entity.User;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.entity.enums.VerificationStatus;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.ExecutorProfileRepository;
import kg.freelance.repository.ExecutorVerificationRepository;
import kg.freelance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutorVerificationService Tests")
class ExecutorVerificationServiceTest {

    @Mock
    private ExecutorVerificationRepository verificationRepository;

    @Mock
    private ExecutorProfileRepository executorProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ExecutorVerificationService verificationService;

    private User user;
    private User admin;
    private ExecutorVerification verification;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Test User")
                .phone("+996700123456")
                .role(UserRole.USER)
                .executorVerified(false)
                .active(true)
                .build();

        admin = User.builder()
                .id(99L)
                .email("admin@example.com")
                .fullName("Admin User")
                .role(UserRole.ADMIN)
                .build();

        verification = ExecutorVerification.builder()
                .user(user)
                .passportUrl("https://example.com/passport.jpg")
                .selfieUrl("https://example.com/selfie.jpg")
                .status(VerificationStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();
        verification.setUserId(1L); // Manually set since @MapsId won't work in test
    }

    @Nested
    @DisplayName("Is Verified Tests")
    class IsVerifiedTests {

        @Test
        @DisplayName("Should return true when user is verified")
        void shouldReturnTrueWhenUserIsVerified() {
            // Given
            user.setExecutorVerified(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // When
            boolean result = verificationService.isVerified(1L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when user is not verified")
        void shouldReturnFalseWhenUserIsNotVerified() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // When
            boolean result = verificationService.isVerified(1L);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when user ID is null")
        void shouldReturnFalseWhenUserIdIsNull() {
            // When
            boolean result = verificationService.isVerified(null);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when user not found")
        void shouldReturnFalseWhenUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            boolean result = verificationService.isVerified(999L);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Get My Status Tests")
    class GetMyStatusTests {

        @Test
        @DisplayName("Should return verification status")
        void shouldReturnVerificationStatus() {
            // Given
            when(verificationRepository.findByUserId(1L)).thenReturn(Optional.of(verification));

            // When
            VerificationResponse result = verificationService.getMyStatus(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(VerificationStatus.PENDING);
        }

        @Test
        @DisplayName("Should return NONE status when no verification exists")
        void shouldReturnNoneStatusWhenNoVerificationExists() {
            // Given
            when(verificationRepository.findByUserId(1L)).thenReturn(Optional.empty());

            // When
            VerificationResponse result = verificationService.getMyStatus(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(VerificationStatus.NONE);
        }
    }

    @Nested
    @DisplayName("Submit Verification Tests")
    class SubmitVerificationTests {

        @Test
        @DisplayName("Should submit verification successfully")
        void shouldSubmitVerificationSuccessfully() {
            // Given
            VerificationSubmitRequest request = new VerificationSubmitRequest();
            request.setPassportUrl("https://example.com/passport.jpg");
            request.setSelfieUrl("https://example.com/selfie.jpg");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(verificationRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(verificationRepository.save(any(ExecutorVerification.class))).thenAnswer(inv -> {
                ExecutorVerification v = inv.getArgument(0);
                v.setSubmittedAt(LocalDateTime.now());
                return v;
            });

            // When
            VerificationResponse result = verificationService.submitVerification(1L, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(VerificationStatus.PENDING);
            verify(verificationRepository).save(any(ExecutorVerification.class));
        }

        @Test
        @DisplayName("Should throw exception when user already verified")
        void shouldThrowExceptionWhenUserAlreadyVerified() {
            // Given
            user.setExecutorVerified(true);
            VerificationSubmitRequest request = new VerificationSubmitRequest();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // When/Then
            assertThatThrownBy(() -> verificationService.submitVerification(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("User is already verified");
        }

        @Test
        @DisplayName("Should throw exception when verification already pending")
        void shouldThrowExceptionWhenVerificationAlreadyPending() {
            // Given
            VerificationSubmitRequest request = new VerificationSubmitRequest();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(verificationRepository.findByUserId(1L)).thenReturn(Optional.of(verification));

            // When/Then
            assertThatThrownBy(() -> verificationService.submitVerification(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Verification request already pending");
        }

        @Test
        @DisplayName("Should allow resubmission after rejection")
        void shouldAllowResubmissionAfterRejection() {
            // Given
            verification.setStatus(VerificationStatus.REJECTED);
            VerificationSubmitRequest request = new VerificationSubmitRequest();
            request.setPassportUrl("https://example.com/new-passport.jpg");
            request.setSelfieUrl("https://example.com/new-selfie.jpg");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(verificationRepository.findByUserId(1L)).thenReturn(Optional.of(verification));
            when(verificationRepository.save(any(ExecutorVerification.class))).thenReturn(verification);

            // When
            VerificationResponse result = verificationService.submitVerification(1L, request);

            // Then
            assertThat(result).isNotNull();
            verify(verificationRepository).save(any(ExecutorVerification.class));
        }
    }

    @Nested
    @DisplayName("Admin Get Verifications Tests")
    class AdminGetVerificationsTests {

        @Test
        @DisplayName("Should get pending verifications")
        void shouldGetPendingVerifications() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ExecutorVerification> page = new PageImpl<>(List.of(verification), pageable, 1);

            when(verificationRepository.findByStatusOrderBySubmittedAtDesc(VerificationStatus.PENDING, pageable))
                    .thenReturn(page);

            // When
            PageResponse<AdminVerificationResponse> result = verificationService.getPendingVerifications(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should get all verifications")
        void shouldGetAllVerifications() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ExecutorVerification> page = new PageImpl<>(List.of(verification), pageable, 1);

            when(verificationRepository.findAllByOrderBySubmittedAtDesc(pageable)).thenReturn(page);

            // When
            PageResponse<AdminVerificationResponse> result = verificationService.getAllVerifications(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should get verification details")
        void shouldGetVerificationDetails() {
            // Given
            when(verificationRepository.findByUserId(1L)).thenReturn(Optional.of(verification));

            // When
            AdminVerificationResponse result = verificationService.getVerificationDetails(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(VerificationStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("Approve Verification Tests")
    class ApproveVerificationTests {

        @Test
        @DisplayName("Should approve verification successfully")
        void shouldApproveVerificationSuccessfully() {
            // Given
            when(verificationRepository.findByUserId(1L)).thenReturn(Optional.of(verification));
            when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
            when(executorProfileRepository.existsById(1L)).thenReturn(false);

            // When
            verificationService.approveVerification(1L, 99L);

            // Then
            verify(userRepository).save(argThat(u -> u.getExecutorVerified()));
            verify(executorProfileRepository).save(any(ExecutorProfile.class));
            verify(verificationRepository).save(argThat(v ->
                    v.getStatus() == VerificationStatus.APPROVED &&
                    v.getReviewedBy() != null
            ));
            verify(emailService).sendVerificationApproved(user);
        }

        @Test
        @DisplayName("Should not create profile if already exists")
        void shouldNotCreateProfileIfAlreadyExists() {
            // Given
            when(verificationRepository.findByUserId(1L)).thenReturn(Optional.of(verification));
            when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
            when(executorProfileRepository.existsById(1L)).thenReturn(true);

            // When
            verificationService.approveVerification(1L, 99L);

            // Then
            verify(executorProfileRepository, never()).save(any(ExecutorProfile.class));
        }
    }

    @Nested
    @DisplayName("Reject Verification Tests")
    class RejectVerificationTests {

        @Test
        @DisplayName("Should reject verification successfully")
        void shouldRejectVerificationSuccessfully() {
            // Given
            when(verificationRepository.findByUserId(1L)).thenReturn(Optional.of(verification));
            when(userRepository.findById(99L)).thenReturn(Optional.of(admin));

            // When
            verificationService.rejectVerification(1L, 99L, "Documents unclear");

            // Then
            verify(verificationRepository).save(argThat(v ->
                    v.getStatus() == VerificationStatus.REJECTED &&
                    v.getRejectionReason().equals("Documents unclear") &&
                    v.getReviewedBy() != null
            ));
            verify(emailService).sendVerificationRejected(user, "Documents unclear");
        }

        @Test
        @DisplayName("Should throw exception when verification not found")
        void shouldThrowExceptionWhenVerificationNotFound() {
            // Given
            when(verificationRepository.findByUserId(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> verificationService.rejectVerification(999L, 99L, "Reason"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Count Pending Tests")
    class CountPendingTests {

        @Test
        @DisplayName("Should count pending verifications")
        void shouldCountPendingVerifications() {
            // Given
            when(verificationRepository.countByStatus(VerificationStatus.PENDING)).thenReturn(5L);

            // When
            long result = verificationService.countPending();

            // Then
            assertThat(result).isEqualTo(5);
        }
    }
}
