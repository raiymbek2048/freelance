package kg.freelance.service;

import kg.freelance.dto.response.NotificationResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.entity.Order;
import kg.freelance.entity.User;
import kg.freelance.entity.enums.NotificationType;
import org.springframework.data.domain.Pageable;

public interface InAppNotificationService {

    void send(User recipient, NotificationType type, String title, String message, Order order, String link);

    PageResponse<NotificationResponse> getNotifications(Long userId, Pageable pageable);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);
}
