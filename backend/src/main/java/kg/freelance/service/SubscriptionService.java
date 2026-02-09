package kg.freelance.service;

import kg.freelance.dto.request.GrantSubscriptionRequest;
import kg.freelance.dto.request.SubscriptionSettingsRequest;
import kg.freelance.dto.response.AnnouncementResponse;
import kg.freelance.dto.response.MySubscriptionResponse;
import kg.freelance.dto.response.SubscriptionSettingsResponse;
import kg.freelance.dto.response.UserSubscriptionResponse;
import kg.freelance.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubscriptionService {

    // Access checks
    boolean hasActiveSubscription(User user);
    boolean isSubscriptionRequired();
    boolean canAccessOrders(User user);

    // User methods
    MySubscriptionResponse getMySubscription(Long userId);
    MySubscriptionResponse startTrial(Long userId);

    // Admin methods
    SubscriptionSettingsResponse getSettings();
    SubscriptionSettingsResponse updateSettings(SubscriptionSettingsRequest request, Long adminId);
    Page<UserSubscriptionResponse> getAllSubscriptions(Pageable pageable);
    UserSubscriptionResponse grantSubscription(Long userId, GrantSubscriptionRequest request, Long adminId);
    void revokeSubscription(Long userId);

    // Payment
    void activatePaidSubscription(Long userId, int days, String paymentReference);

    // Public
    AnnouncementResponse getAnnouncement();
}
