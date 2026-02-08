package kg.freelance.service.impl;

import kg.freelance.dto.request.GrantSubscriptionRequest;
import kg.freelance.dto.request.SubscriptionSettingsRequest;
import kg.freelance.dto.response.AnnouncementResponse;
import kg.freelance.dto.response.MySubscriptionResponse;
import kg.freelance.dto.response.SubscriptionSettingsResponse;
import kg.freelance.dto.response.UserSubscriptionResponse;
import kg.freelance.entity.SubscriptionSettings;
import kg.freelance.entity.User;
import kg.freelance.entity.UserSubscription;
import kg.freelance.entity.enums.SubscriptionStatus;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.SubscriptionSettingsRepository;
import kg.freelance.repository.UserRepository;
import kg.freelance.repository.UserSubscriptionRepository;
import kg.freelance.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionSettingsRepository settingsRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(User user) {
        return subscriptionRepository.findActiveSubscription(user, LocalDateTime.now()).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSubscriptionRequired() {
        SubscriptionSettings settings = settingsRepository.getSettings();
        if (settings.getSubscriptionStartDate() == null) {
            return false;
        }
        return !LocalDate.now().isBefore(settings.getSubscriptionStartDate());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccessOrders(User user) {
        // Always require verification
        if (!user.getExecutorVerified()) {
            return false;
        }
        // If subscription is not required yet - access is granted
        if (!isSubscriptionRequired()) {
            return true;
        }
        // Check for active subscription
        return hasActiveSubscription(user);
    }

    @Override
    @Transactional(readOnly = true)
    public MySubscriptionResponse getMySubscription(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        SubscriptionSettings settings = settingsRepository.getSettings();

        var activeSubscription = subscriptionRepository.findActiveSubscription(user, LocalDateTime.now());
        boolean hasUsedTrial = subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.TRIAL);

        if (activeSubscription.isPresent()) {
            UserSubscription sub = activeSubscription.get();
            int daysRemaining = (int) ChronoUnit.DAYS.between(LocalDateTime.now(), sub.getEndDate());
            return MySubscriptionResponse.builder()
                    .hasActiveSubscription(true)
                    .subscriptionRequired(isSubscriptionRequired())
                    .status(sub.getStatus())
                    .endDate(sub.getEndDate())
                    .daysRemaining(Math.max(0, daysRemaining))
                    .price(settings.getPrice())
                    .canStartTrial(false)
                    .trialDays(settings.getTrialDays())
                    .build();
        }

        return MySubscriptionResponse.builder()
                .hasActiveSubscription(false)
                .subscriptionRequired(isSubscriptionRequired())
                .status(null)
                .endDate(null)
                .daysRemaining(null)
                .price(settings.getPrice())
                .canStartTrial(!hasUsedTrial && settings.getTrialDays() > 0)
                .trialDays(settings.getTrialDays())
                .build();
    }

    @Override
    @Transactional
    public MySubscriptionResponse startTrial(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        SubscriptionSettings settings = settingsRepository.getSettings();

        // Check if user already used trial
        if (subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.TRIAL)) {
            throw new BadRequestException("Trial period has already been used");
        }

        // Check if trial is available
        if (settings.getTrialDays() == null || settings.getTrialDays() <= 0) {
            throw new BadRequestException("Trial period is not available");
        }

        // Create trial subscription
        LocalDateTime now = LocalDateTime.now();
        UserSubscription subscription = UserSubscription.builder()
                .user(user)
                .status(SubscriptionStatus.TRIAL)
                .startDate(now)
                .endDate(now.plusDays(settings.getTrialDays()))
                .build();

        subscriptionRepository.save(subscription);
        log.info("User {} started trial subscription", userId);

        return getMySubscription(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionSettingsResponse getSettings() {
        SubscriptionSettings settings = settingsRepository.getSettings();
        return mapToSettingsResponse(settings);
    }

    @Override
    @Transactional
    public SubscriptionSettingsResponse updateSettings(SubscriptionSettingsRequest request, Long adminId) {
        SubscriptionSettings settings = settingsRepository.getSettings();
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        if (request.getPrice() != null) {
            settings.setPrice(request.getPrice());
        }
        if (request.getSubscriptionStartDate() != null) {
            settings.setSubscriptionStartDate(request.getSubscriptionStartDate());
        }
        if (request.getTrialDays() != null) {
            settings.setTrialDays(request.getTrialDays());
        }
        if (request.getAnnouncementMessage() != null) {
            settings.setAnnouncementMessage(request.getAnnouncementMessage());
        }
        if (request.getAnnouncementEnabled() != null) {
            settings.setAnnouncementEnabled(request.getAnnouncementEnabled());
        }

        settings.setUpdatedBy(admin);
        settings = settingsRepository.save(settings);

        log.info("Subscription settings updated by admin {}", adminId);
        return mapToSettingsResponse(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSubscriptionResponse> getAllSubscriptions(Pageable pageable) {
        return subscriptionRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToSubscriptionResponse);
    }

    @Override
    @Transactional
    public UserSubscriptionResponse grantSubscription(Long userId, GrantSubscriptionRequest request, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        // Expire any existing active subscriptions
        var existing = subscriptionRepository.findActiveSubscription(user, LocalDateTime.now());
        existing.ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
        });

        // Create new subscription
        LocalDateTime now = LocalDateTime.now();
        UserSubscription subscription = UserSubscription.builder()
                .user(user)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(now.plusDays(request.getDays()))
                .grantedByAdmin(admin.getFullName())
                .build();

        subscription = subscriptionRepository.save(subscription);
        log.info("Admin {} granted {} days subscription to user {}", adminId, request.getDays(), userId);

        return mapToSubscriptionResponse(subscription);
    }

    @Override
    @Transactional
    public void revokeSubscription(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        var existing = subscriptionRepository.findActiveSubscription(user, LocalDateTime.now());
        if (existing.isEmpty()) {
            throw new BadRequestException("User has no active subscription");
        }

        UserSubscription subscription = existing.get();
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscription.setEndDate(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        log.info("Subscription revoked for user {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementResponse getAnnouncement() {
        SubscriptionSettings settings = settingsRepository.getSettings();
        return AnnouncementResponse.builder()
                .message(settings.getAnnouncementMessage())
                .enabled(settings.getAnnouncementEnabled())
                .build();
    }

    private SubscriptionSettingsResponse mapToSettingsResponse(SubscriptionSettings settings) {
        return SubscriptionSettingsResponse.builder()
                .price(settings.getPrice())
                .subscriptionStartDate(settings.getSubscriptionStartDate())
                .trialDays(settings.getTrialDays())
                .announcementMessage(settings.getAnnouncementMessage())
                .announcementEnabled(settings.getAnnouncementEnabled())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    private UserSubscriptionResponse mapToSubscriptionResponse(UserSubscription subscription) {
        User user = subscription.getUser();
        int daysRemaining = (int) ChronoUnit.DAYS.between(LocalDateTime.now(), subscription.getEndDate());
        boolean isActive = subscription.getStatus() != SubscriptionStatus.EXPIRED
                && subscription.getEndDate().isAfter(LocalDateTime.now());

        return UserSubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(user.getId())
                .userFullName(user.getFullName())
                .userEmail(user.getEmail())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .daysRemaining(Math.max(0, daysRemaining))
                .isActive(isActive)
                .build();
    }
}
