package kg.freelance.service.impl;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import kg.freelance.entity.User;
import kg.freelance.repository.UserRepository;
import kg.freelance.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationServiceImpl implements PushNotificationService {

    private final UserRepository userRepository;

    @Override
    @Async
    public void sendPush(User recipient, String title, String body, Map<String, String> data) {
        if (recipient.getFcmToken() == null || recipient.getFcmToken().isBlank()) {
            return;
        }
        if (FirebaseApp.getApps().isEmpty()) {
            log.debug("Firebase not initialized, skipping push notification");
            return;
        }
        try {
            Message.Builder builder = Message.builder()
                    .setToken(recipient.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());
            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }
            String messageId = FirebaseMessaging.getInstance().send(builder.build());
            log.debug("FCM push sent to user {}: {}", recipient.getId(), messageId);
        } catch (Exception e) {
            log.warn("Failed to send FCM push to user {}: {}", recipient.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateFcmToken(Long userId, String token) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFcmToken(token);
            userRepository.save(user);
        });
    }
}
