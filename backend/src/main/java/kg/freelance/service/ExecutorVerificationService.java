package kg.freelance.service;

import kg.freelance.dto.request.VerificationSubmitRequest;
import kg.freelance.dto.response.AdminVerificationResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.dto.response.VerificationResponse;
import kg.freelance.entity.ExecutorProfile;
import kg.freelance.entity.ExecutorVerification;
import kg.freelance.entity.User;
import kg.freelance.entity.enums.VerificationStatus;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.ExecutorProfileRepository;
import kg.freelance.repository.ExecutorVerificationRepository;
import kg.freelance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExecutorVerificationService {

    private final ExecutorVerificationRepository verificationRepository;
    private final ExecutorProfileRepository executorProfileRepository;
    private final UserRepository userRepository;

    public boolean isVerified(Long userId) {
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(User::getExecutorVerified)
                .orElse(false);
    }

    public VerificationResponse getMyStatus(Long userId) {
        return verificationRepository.findByUserId(userId)
                .map(this::mapToResponse)
                .orElse(VerificationResponse.builder()
                        .status(VerificationStatus.NONE)
                        .build());
    }

    @Transactional
    public VerificationResponse submitVerification(Long userId, VerificationSubmitRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getExecutorVerified()) {
            throw new BadRequestException("User is already verified");
        }

        ExecutorVerification verification = verificationRepository.findByUserId(userId)
                .orElse(new ExecutorVerification());

        // Only check pending status if verification record already exists
        if (verification.getUserId() != null && verification.getStatus() == VerificationStatus.PENDING) {
            throw new BadRequestException("Verification request already pending");
        }

        verification.setUser(user);
        verification.setPassportUrl(request.getPassportUrl());
        verification.setSelfieUrl(request.getSelfieUrl());
        verification.setStatus(VerificationStatus.PENDING);
        verification.setRejectionReason(null);
        verification.setReviewedAt(null);
        verification.setReviewedBy(null);

        verificationRepository.save(verification);

        return mapToResponse(verification);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminVerificationResponse> getPendingVerifications(Pageable pageable) {
        Page<ExecutorVerification> page = verificationRepository
                .findByStatusOrderBySubmittedAtDesc(VerificationStatus.PENDING, pageable);
        return mapToPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminVerificationResponse> getAllVerifications(Pageable pageable) {
        Page<ExecutorVerification> page = verificationRepository.findAllByOrderBySubmittedAtDesc(pageable);
        return mapToPageResponse(page);
    }

    @Transactional(readOnly = true)
    public AdminVerificationResponse getVerificationDetails(Long userId) {
        ExecutorVerification verification = verificationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found"));
        return mapToAdminResponse(verification);
    }

    @Transactional
    public void approveVerification(Long userId, Long adminId) {
        ExecutorVerification verification = verificationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        verification.setStatus(VerificationStatus.APPROVED);
        verification.setReviewedAt(LocalDateTime.now());
        verification.setReviewedBy(admin);
        verification.setRejectionReason(null);

        // Update user's verified status
        User user = verification.getUser();
        user.setExecutorVerified(true);
        userRepository.save(user);

        // Create executor profile if not exists
        if (!executorProfileRepository.existsById(user.getId())) {
            ExecutorProfile profile = ExecutorProfile.builder()
                    .user(user)
                    .availableForWork(true)
                    .build();
            executorProfileRepository.save(profile);
        }

        verificationRepository.save(verification);
    }

    @Transactional
    public void rejectVerification(Long userId, Long adminId, String reason) {
        ExecutorVerification verification = verificationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        verification.setStatus(VerificationStatus.REJECTED);
        verification.setReviewedAt(LocalDateTime.now());
        verification.setReviewedBy(admin);
        verification.setRejectionReason(reason);

        verificationRepository.save(verification);
    }

    public long countPending() {
        return verificationRepository.countByStatus(VerificationStatus.PENDING);
    }

    private VerificationResponse mapToResponse(ExecutorVerification v) {
        return VerificationResponse.builder()
                .status(v.getStatus())
                .submittedAt(v.getSubmittedAt())
                .reviewedAt(v.getReviewedAt())
                .rejectionReason(v.getRejectionReason())
                .build();
    }

    private AdminVerificationResponse mapToAdminResponse(ExecutorVerification v) {
        User user = v.getUser();
        return AdminVerificationResponse.builder()
                .userId(user.getId())
                .userFullName(user.getFullName())
                .userEmail(user.getEmail())
                .userPhone(user.getPhone())
                .userAvatarUrl(user.getAvatarUrl())
                .status(v.getStatus())
                .passportUrl(v.getPassportUrl())
                .selfieUrl(v.getSelfieUrl())
                .rejectionReason(v.getRejectionReason())
                .submittedAt(v.getSubmittedAt())
                .reviewedAt(v.getReviewedAt())
                .reviewedByName(v.getReviewedBy() != null ? v.getReviewedBy().getFullName() : null)
                .build();
    }

    private PageResponse<AdminVerificationResponse> mapToPageResponse(Page<ExecutorVerification> page) {
        return PageResponse.<AdminVerificationResponse>builder()
                .content(page.getContent().stream().map(this::mapToAdminResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
