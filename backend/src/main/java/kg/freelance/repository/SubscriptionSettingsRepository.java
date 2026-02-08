package kg.freelance.repository;

import kg.freelance.entity.SubscriptionSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface SubscriptionSettingsRepository extends JpaRepository<SubscriptionSettings, Long> {

    default SubscriptionSettings getSettings() {
        return findById(1L).orElseGet(() -> {
            SubscriptionSettings settings = SubscriptionSettings.builder()
                    .id(1L)
                    .price(BigDecimal.valueOf(500))
                    .trialDays(7)
                    .announcementEnabled(false)
                    .build();
            return save(settings);
        });
    }
}
