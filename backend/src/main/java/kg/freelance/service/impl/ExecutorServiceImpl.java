package kg.freelance.service.impl;

import kg.freelance.dto.request.ExecutorProfileRequest;
import kg.freelance.dto.response.*;
import kg.freelance.entity.Category;
import kg.freelance.entity.ExecutorProfile;
import kg.freelance.entity.Review;
import kg.freelance.entity.User;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.CategoryRepository;
import kg.freelance.repository.ExecutorProfileRepository;
import kg.freelance.repository.PortfolioRepository;
import kg.freelance.repository.ReviewRepository;
import kg.freelance.repository.UserRepository;
import kg.freelance.entity.enums.ReputationLevel;
import kg.freelance.service.ExecutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExecutorServiceImpl implements ExecutorService {

    private final ExecutorProfileRepository executorProfileRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final PortfolioRepository portfolioRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ExecutorListResponse> getExecutors(
            Long categoryId,
            BigDecimal minRating,
            Boolean availableOnly,
            String search,
            Pageable pageable) {

        Page<ExecutorProfile> page = executorProfileRepository.findExecutors(
                categoryId,
                minRating,
                availableOnly != null && availableOnly,
                search,
                pageable
        );

        List<ExecutorListResponse> content = page.getContent().stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public ExecutorResponse getExecutorById(Long id) {
        ExecutorProfile profile = executorProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Executor", "id", id));

        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public ExecutorResponse createOrUpdateProfile(Long userId, ExecutorProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        ExecutorProfile profile = executorProfileRepository.findById(userId)
                .orElseGet(() -> {
                    ExecutorProfile newProfile = new ExecutorProfile();
                    newProfile.setUser(user);
                    newProfile.setTotalOrders(0);
                    newProfile.setCompletedOrders(0);
                    newProfile.setDisputedOrders(0);
                    newProfile.setAvgCompletionDays(0.0);
                    newProfile.setRating(BigDecimal.ZERO);
                    newProfile.setReviewCount(0);
                    newProfile.setAvailableForWork(true);
                    return newProfile;
                });

        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getSpecialization() != null) {
            profile.setSpecialization(request.getSpecialization());
        }
        if (request.getAvailableForWork() != null) {
            profile.setAvailableForWork(request.getAvailableForWork());
        }
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
            profile.setCategories(categories);
        }

        profile.setLastActiveAt(LocalDateTime.now());
        profile = executorProfileRepository.save(profile);

        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public ExecutorResponse updateCategories(Long userId, List<Long> categoryIds) {
        ExecutorProfile profile = executorProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Executor profile", "userId", userId));

        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(categoryIds));
        profile.setCategories(categories);
        profile = executorProfileRepository.save(profile);

        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public void updateAvailability(Long userId, boolean available) {
        ExecutorProfile profile = executorProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Executor profile", "userId", userId));

        profile.setAvailableForWork(available);
        profile.setLastActiveAt(LocalDateTime.now());
        executorProfileRepository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getExecutorReviews(Long executorId, Pageable pageable) {
        if (!executorProfileRepository.existsById(executorId)) {
            throw new ResourceNotFoundException("Executor", "id", executorId);
        }

        Page<Review> page = reviewRepository.findByExecutorIdAndIsVisibleTrueOrderByCreatedAtDesc(executorId, pageable);

        List<ReviewResponse> content = page.getContent().stream()
                .map(this::mapReviewToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getExecutorPortfolio(Long executorId) {
        if (!executorProfileRepository.existsById(executorId)) {
            throw new ResourceNotFoundException("Executor", "id", executorId);
        }

        return portfolioRepository.findByExecutorIdOrderBySortOrder(executorId).stream()
                .map(portfolio -> PortfolioResponse.builder()
                        .id(portfolio.getId())
                        .title(portfolio.getTitle())
                        .description(portfolio.getDescription())
                        .categoryId(portfolio.getCategory() != null ? portfolio.getCategory().getId() : null)
                        .categoryName(portfolio.getCategory() != null ? portfolio.getCategory().getName() : null)
                        .images(portfolio.getImages())
                        .externalLink(portfolio.getExternalLink())
                        .sortOrder(portfolio.getSortOrder())
                        .createdAt(portfolio.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasExecutorProfile(Long userId) {
        return executorProfileRepository.existsById(userId);
    }

    private ExecutorListResponse mapToListResponse(ExecutorProfile profile) {
        User user = profile.getUser();
        List<CategoryResponse> categories = profile.getCategories().stream()
                .map(cat -> CategoryResponse.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .slug(cat.getSlug())
                        .build())
                .toList();

        ReputationLevel reputation = ReputationLevel.calculate(profile.getCompletedOrders(), profile.getRating());

        return ExecutorListResponse.builder()
                .id(profile.getId())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(profile.getBio())
                .specialization(profile.getSpecialization())
                .completedOrders(profile.getCompletedOrders())
                .rating(profile.getRating())
                .reviewCount(profile.getReviewCount())
                .availableForWork(profile.getAvailableForWork())
                .categories(categories)
                .reputationLevel(reputation.getLabel())
                .reputationColor(reputation.getColor())
                .build();
    }

    private ExecutorResponse mapToResponse(ExecutorProfile profile) {
        User user = profile.getUser();
        List<CategoryResponse> categories = profile.getCategories().stream()
                .map(cat -> CategoryResponse.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .slug(cat.getSlug())
                        .build())
                .collect(Collectors.toList());

        ReputationLevel reputation = ReputationLevel.calculate(profile.getCompletedOrders(), profile.getRating());

        return ExecutorResponse.builder()
                .id(profile.getId())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .whatsappLink(user.getWhatsappLink())
                .bio(profile.getBio())
                .specialization(profile.getSpecialization())
                .totalOrders(profile.getTotalOrders())
                .completedOrders(profile.getCompletedOrders())
                .avgCompletionDays(profile.getAvgCompletionDays())
                .rating(profile.getRating())
                .reviewCount(profile.getReviewCount())
                .availableForWork(profile.getAvailableForWork())
                .lastActiveAt(profile.getLastActiveAt())
                .memberSince(user.getCreatedAt())
                .categories(categories)
                .reputationLevel(reputation.getLabel())
                .reputationColor(reputation.getColor())
                .build();
    }

    private ReviewResponse mapReviewToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .orderId(review.getOrder().getId())
                .orderTitle(review.getOrder().getTitle())
                .clientId(review.getClient().getId())
                .clientName(review.getClient().getFullName())
                .clientAvatarUrl(review.getClient().getAvatarUrl())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
