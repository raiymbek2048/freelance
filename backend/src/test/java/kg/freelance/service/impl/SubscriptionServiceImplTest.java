package kg.freelance.service.impl;

import kg.freelance.dto.request.GrantSubscriptionRequest;
import kg.freelance.dto.request.SubscriptionSettingsRequest;
import kg.freelance.dto.response.MySubscriptionResponse;
import kg.freelance.dto.response.SubscriptionSettingsResponse;
import kg.freelance.dto.response.UserSubscriptionResponse;
import kg.freelance.entity.SubscriptionSettings;
import kg.freelance.entity.User;
import kg.freelance.entity.UserSubscription;
import kg.freelance.entity.enums.SubscriptionStatus;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.SubscriptionSettingsRepository;
import kg.freelance.repository.UserRepository;
import kg.freelance.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService Tests")
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionSettingsRepository settingsRepository;

    @Mock
    private UserSubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private User testUser;
    private SubscriptionSettings settings;
    private UserSubscription activeSubscription;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .role(UserRole.USER)
                .active(true)
                .executorVerified(true)
                .build();

        settings = new SubscriptionSettings();
        settings.setId(1L);
        settings.setPrice(BigDecimal.valueOf(500));
        settings.setTrialDays(7);
        settings.setSubscriptionStartDate(LocalDate.now().minusDays(1)); // Subscription required
        settings.setAnnouncementEnabled(false);

        activeSubscription = UserSubscription.builder()
                .id(1L)
                .user(testUser)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now().minusDays(5))
                .endDate(LocalDateTime.now().plusDays(25))
                .build();
    }

    @Nested
    @DisplayName("Has Active Subscription Tests")
    class HasActiveSubscriptionTests {

        @Test
        @DisplayName("Should return true when user has active subscription")
        void shouldReturnTrueWhenUserHasActiveSubscription() {
            // Given
            when(subscriptionRepository.findActiveSubscription(eq(testUser), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(activeSubscription));

            // When
            boolean result = subscriptionService.hasActiveSubscription(testUser);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when user has no active subscription")
        void shouldReturnFalseWhenNoActiveSubscription() {
            // Given
            when(subscriptionRepository.findActiveSubscription(eq(testUser), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            // When
            boolean result = subscriptionService.hasActiveSubscription(testUser);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Is Subscription Required Tests")
    class IsSubscriptionRequiredTests {

        @Test
        @DisplayName("Should return true when start date is in the past")
        void shouldReturnTrueWhenStartDateInPast() {
            // Given
            settings.setSubscriptionStartDate(LocalDate.now().minusDays(1));
            when(settingsRepository.getSettings()).thenReturn(settings);

            // When
            boolean result = subscriptionService.isSubscriptionRequired();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when start date is in the future")
        void shouldReturnFalseWhenStartDateInFuture() {
            // Given
            settings.setSubscriptionStartDate(LocalDate.now().plusDays(30));
            when(settingsRepository.getSettings()).thenReturn(settings);

            // When
            boolean result = subscriptionService.isSubscriptionRequired();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when start date is null")
        void shouldReturnFalseWhenStartDateNull() {
            // Given
            settings.setSubscriptionStartDate(null);
            when(settingsRepository.getSettings()).thenReturn(settings);

            // When
            boolean result = subscriptionService.isSubscriptionRequired();

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Can Access Orders Tests")
    class CanAccessOrdersTests {

        @Test
        @DisplayName("Should return false when user not verified")
        void shouldReturnFalseWhenNotVerified() {
            // Given
            testUser.setExecutorVerified(false);

            // When
            boolean result = subscriptionService.canAccessOrders(testUser);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when subscription not required")
        void shouldReturnTrueWhenSubscriptionNotRequired() {
            // Given
            settings.setSubscriptionStartDate(null);
            when(settingsRepository.getSettings()).thenReturn(settings);

            // When
            boolean result = subscriptionService.canAccessOrders(testUser);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when has active subscription")
        void shouldReturnTrueWhenHasActiveSubscription() {
            // Given
            settings.setSubscriptionStartDate(LocalDate.now().minusDays(1));
            when(settingsRepository.getSettings()).thenReturn(settings);
            when(subscriptionRepository.findActiveSubscription(eq(testUser), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(activeSubscription));

            // When
            boolean result = subscriptionService.canAccessOrders(testUser);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when subscription required but not active")
        void shouldReturnFalseWhenSubscriptionRequiredButNotActive() {
            // Given
            settings.setSubscriptionStartDate(LocalDate.now().minusDays(1));
            when(settingsRepository.getSettings()).thenReturn(settings);
            when(subscriptionRepository.findActiveSubscription(eq(testUser), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            // When
            boolean result = subscriptionService.canAccessOrders(testUser);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Get My Subscription Tests")
    class GetMySubscriptionTests {

        @Test
        @DisplayName("Should return subscription info with active subscription")
        void shouldReturnSubscriptionInfoWithActiveSubscription() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(settingsRepository.getSettings()).thenReturn(settings);
            when(subscriptionRepository.findActiveSubscription(eq(testUser), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(activeSubscription));
            when(subscriptionRepository.existsByUserAndStatus(testUser, SubscriptionStatus.TRIAL))
                    .thenReturn(false);

            // When
            MySubscriptionResponse response = subscriptionService.getMySubscription(1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getHasActiveSubscription()).isTrue();
            assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should return can start trial when no previous trial")
        void shouldReturnCanStartTrialWhenNoPreviousTrial() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(settingsRepository.getSettings()).thenReturn(settings);
            when(subscriptionRepository.findActiveSubscription(eq(testUser), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.existsByUserAndStatus(testUser, SubscriptionStatus.TRIAL))
                    .thenReturn(false);

            // When
            MySubscriptionResponse response = subscriptionService.getMySubscription(1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getCanStartTrial()).isTrue();
        }
    }

    @Nested
    @DisplayName("Start Trial Tests")
    class StartTrialTests {

        @Test
        @DisplayName("Should start trial successfully")
        void shouldStartTrialSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(settingsRepository.getSettings()).thenReturn(settings);
            when(subscriptionRepository.existsByUserAndStatus(testUser, SubscriptionStatus.TRIAL))
                    .thenReturn(false);
            when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(invocation -> {
                UserSubscription sub = invocation.getArgument(0);
                sub.setId(1L);
                return sub;
            });
            when(subscriptionRepository.findActiveSubscription(eq(testUser), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(activeSubscription));

            // When
            MySubscriptionResponse response = subscriptionService.startTrial(1L);

            // Then
            assertThat(response).isNotNull();
            verify(subscriptionRepository).save(argThat(sub ->
                    sub.getStatus() == SubscriptionStatus.TRIAL
            ));
        }

        @Test
        @DisplayName("Should throw exception when trial already used")
        void shouldThrowExceptionWhenTrialAlreadyUsed() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(subscriptionRepository.existsByUserAndStatus(testUser, SubscriptionStatus.TRIAL))
                    .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> subscriptionService.startTrial(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Trial period has already been used");
        }
    }

    @Nested
    @DisplayName("Admin Grant Subscription Tests")
    class AdminGrantSubscriptionTests {

        @Test
        @DisplayName("Should grant subscription successfully")
        void shouldGrantSubscriptionSuccessfully() {
            // Given
            GrantSubscriptionRequest request = new GrantSubscriptionRequest();
            request.setDays(30);

            User admin = User.builder()
                    .id(99L)
                    .email("admin@example.com")
                    .fullName("Admin")
                    .role(UserRole.ADMIN)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
            when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(invocation -> {
                UserSubscription sub = invocation.getArgument(0);
                sub.setId(1L);
                sub.setCreatedAt(LocalDateTime.now());
                return sub;
            });

            // When
            UserSubscriptionResponse response = subscriptionService.grantSubscription(1L, request, 99L);

            // Then
            assertThat(response).isNotNull();
            verify(subscriptionRepository).save(argThat(sub ->
                    sub.getStatus() == SubscriptionStatus.ACTIVE &&
                    sub.getGrantedByAdmin() != null
            ));
        }
    }

    @Nested
    @DisplayName("Update Settings Tests")
    class UpdateSettingsTests {

        @Test
        @DisplayName("Should update settings successfully")
        void shouldUpdateSettingsSuccessfully() {
            // Given
            SubscriptionSettingsRequest request = new SubscriptionSettingsRequest();
            request.setPrice(BigDecimal.valueOf(1000));
            request.setTrialDays(14);
            request.setSubscriptionStartDate(LocalDate.now().plusDays(7));
            request.setAnnouncementMessage("New announcement");
            request.setAnnouncementEnabled(true);

            User admin = User.builder()
                    .id(99L)
                    .email("admin@example.com")
                    .role(UserRole.ADMIN)
                    .build();

            when(settingsRepository.getSettings()).thenReturn(settings);
            when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
            when(settingsRepository.save(any(SubscriptionSettings.class))).thenReturn(settings);

            // When
            SubscriptionSettingsResponse response = subscriptionService.updateSettings(request, 99L);

            // Then
            assertThat(response).isNotNull();
            verify(settingsRepository).save(any(SubscriptionSettings.class));
        }
    }
}
