package kg.freelance.service;

import kg.freelance.entity.User;

import java.util.Map;

public interface PushNotificationService {

    void sendPush(User recipient, String title, String body, Map<String, String> data);

    void updateFcmToken(Long userId, String token);
}
