package kg.freelance.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;

@Configuration
@Slf4j
public class FcmConfig {

    @Value("${fcm.credentials-path:}")
    private String credentialsPath;

    @PostConstruct
    public void initialize() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("FCM credentials path not configured. Push notifications disabled.");
            return;
        }
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new FileInputStream(credentialsPath)))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            }
        } catch (Exception e) {
            log.warn("Failed to initialize Firebase: {}. Push notifications disabled.", e.getMessage());
        }
    }
}
