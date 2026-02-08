package kg.freelance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionSettings {

    @Id
    @Builder.Default
    private Long id = 1L; // Always single record

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.valueOf(500); // Price in som

    @Column(name = "subscription_start_date")
    private LocalDate subscriptionStartDate; // When subscription becomes required

    @Column(name = "trial_days")
    @Builder.Default
    private Integer trialDays = 7; // Days of free trial

    @Column(name = "announcement_message", columnDefinition = "TEXT")
    private String announcementMessage;

    @Column(name = "announcement_enabled")
    @Builder.Default
    private Boolean announcementEnabled = false;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;
}
