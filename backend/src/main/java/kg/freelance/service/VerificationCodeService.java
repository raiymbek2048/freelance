package kg.freelance.service;

import kg.freelance.entity.enums.VerificationType;

public interface VerificationCodeService {

    /**
     * Send verification code to user's email or phone
     * @param userId user ID
     * @param type EMAIL or PHONE
     */
    void sendVerificationCode(Long userId, VerificationType type);

    /**
     * Verify the code entered by user
     * @param userId user ID
     * @param code verification code
     * @param type EMAIL or PHONE
     * @return true if code is valid
     */
    boolean verifyCode(Long userId, String code, VerificationType type);

    /**
     * Check if user can request a new code (rate limiting)
     * @param userId user ID
     * @param type EMAIL or PHONE
     * @return true if user can request new code
     */
    boolean canRequestNewCode(Long userId, VerificationType type);
}
