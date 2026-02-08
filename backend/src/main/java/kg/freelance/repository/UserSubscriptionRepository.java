package kg.freelance.repository;

import kg.freelance.entity.User;
import kg.freelance.entity.UserSubscription;
import kg.freelance.entity.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    Optional<UserSubscription> findByUserAndStatus(User user, SubscriptionStatus status);

    Optional<UserSubscription> findFirstByUserOrderByEndDateDesc(User user);

    List<UserSubscription> findByStatusAndEndDateBefore(SubscriptionStatus status, LocalDateTime date);

    Page<UserSubscription> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT us FROM UserSubscription us WHERE us.user = :user AND us.status IN ('TRIAL', 'ACTIVE') AND us.endDate > :now")
    Optional<UserSubscription> findActiveSubscription(User user, LocalDateTime now);

    boolean existsByUserAndStatus(User user, SubscriptionStatus status);

    @Query("SELECT us FROM UserSubscription us WHERE us.user.id = :userId ORDER BY us.createdAt DESC")
    List<UserSubscription> findAllByUserId(Long userId);
}
