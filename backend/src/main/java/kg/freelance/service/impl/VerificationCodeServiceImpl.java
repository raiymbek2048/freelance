package kg.freelance.service.impl;

import kg.freelance.entity.User;
import kg.freelance.entity.VerificationCode;
import kg.freelance.entity.enums.VerificationType;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.UserRepository;
import kg.freelance.repository.VerificationCodeRepository;
import kg.freelance.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;

    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRY_MINUTES = 10;
    private static final int MAX_REQUESTS_PER_HOUR = 5;

    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public void sendVerificationCode(Long userId, VerificationType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Check rate limiting
        if (!canRequestNewCode(userId, type)) {
            throw new BadRequestException("Слишком много запросов. Попробуйте позже.");
        }

        // Check if already verified
        if (type == VerificationType.EMAIL && Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email уже подтверждён");
        }
        if (type == VerificationType.PHONE && Boolean.TRUE.equals(user.getPhoneVerified())) {
            throw new BadRequestException("Телефон уже подтверждён");
        }

        // Generate code
        String code = generateCode();

        // Save verification code
        VerificationCode verificationCode = VerificationCode.builder()
                .user(user)
                .code(code)
                .type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES))
                .used(false)
                .build();

        verificationCodeRepository.save(verificationCode);

        // Send code (in development, just log it)
        if (type == VerificationType.EMAIL) {
            sendEmailCode(user.getEmail(), code);
        } else {
            sendSmsCode(user.getPhone(), code);
        }
    }

    @Override
    @Transactional
    public boolean verifyCode(Long userId, String code, VerificationType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        VerificationCode verificationCode = verificationCodeRepository
                .findByUserIdAndCodeAndTypeAndUsedFalseAndExpiresAtAfter(
                        userId, code, type, LocalDateTime.now())
                .orElse(null);

        if (verificationCode == null) {
            return false;
        }

        // Mark code as used
        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);

        // Update user verification status
        if (type == VerificationType.EMAIL) {
            user.setEmailVerified(true);
        } else {
            user.setPhoneVerified(true);
        }
        userRepository.save(user);

        log.info("User {} verified their {}", userId, type);
        return true;
    }

    @Override
    public boolean canRequestNewCode(Long userId, VerificationType type) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long requestCount = verificationCodeRepository.countByUserIdAndTypeAndCreatedAtAfter(
                userId, type, oneHourAgo);
        return requestCount < MAX_REQUESTS_PER_HOUR;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private void sendEmailCode(String email, String code) {
        // TODO: Implement real email sending (e.g., via SMTP or SendGrid)
        // For development, just log the code
        log.info("=================================================");
        log.info("VERIFICATION CODE for EMAIL {}: {}", email, code);
        log.info("=================================================");
    }

    private void sendSmsCode(String phone, String code) {
        // TODO: Implement real SMS sending (e.g., via Nikita.kg or similar)
        // For development, just log the code
        log.info("=================================================");
        log.info("VERIFICATION CODE for PHONE {}: {}", phone, code);
        log.info("=================================================");
    }
}
