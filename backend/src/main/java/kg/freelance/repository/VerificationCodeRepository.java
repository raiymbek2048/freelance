package kg.freelance.repository;

import kg.freelance.entity.VerificationCode;
import kg.freelance.entity.enums.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findByUserIdAndTypeAndUsedFalseAndExpiresAtAfter(
            Long userId, VerificationType type, LocalDateTime now);

    Optional<VerificationCode> findByUserIdAndCodeAndTypeAndUsedFalseAndExpiresAtAfter(
            Long userId, String code, VerificationType type, LocalDateTime now);

    @Modifying
    @Query("UPDATE VerificationCode v SET v.used = true WHERE v.user.id = :userId AND v.type = :type")
    void markAllAsUsed(Long userId, VerificationType type);

    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.expiresAt < :now")
    void deleteExpiredCodes(LocalDateTime now);

    long countByUserIdAndTypeAndCreatedAtAfter(Long userId, VerificationType type, LocalDateTime after);
}
