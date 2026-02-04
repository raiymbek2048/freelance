package kg.freelance.service.impl;

import kg.freelance.dto.request.ReviewRequest;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.dto.response.ReviewResponse;
import kg.freelance.entity.ExecutorProfile;
import kg.freelance.entity.Order;
import kg.freelance.entity.Review;
import kg.freelance.entity.User;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ForbiddenException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.ExecutorProfileRepository;
import kg.freelance.repository.OrderRepository;
import kg.freelance.repository.ReviewRepository;
import kg.freelance.repository.UserRepository;
import kg.freelance.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ExecutorProfileRepository executorProfileRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(Long clientId, Long orderId, ReviewRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Validate client
        if (!order.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("Only the client can leave a review");
        }

        // Check order status
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException("Can only review completed orders");
        }

        // Check if review already exists
        if (reviewRepository.existsByOrderId(orderId)) {
            throw new BadRequestException("Review already exists for this order");
        }

        // Check executor exists
        if (order.getExecutor() == null) {
            throw new BadRequestException("Order has no executor");
        }

        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", clientId));

        Review review = Review.builder()
                .order(order)
                .client(client)
                .executor(order.getExecutor())
                .rating(request.getRating())
                .comment(request.getComment())
                .isModerated(false)
                .isVisible(true)
                .build();

        review = reviewRepository.save(review);

        // Recalculate executor rating
        recalculateExecutorRating(order.getExecutor().getId());

        return mapToResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long clientId, Long reviewId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (!review.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("You can only update your own reviews");
        }

        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }

        // Reset moderation status on update
        review.setIsModerated(false);

        review = reviewRepository.save(review);

        // Recalculate executor rating
        recalculateExecutorRating(review.getExecutor().getId());

        return mapToResponse(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long clientId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (!review.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("You can only delete your own reviews");
        }

        Long executorId = review.getExecutor().getId();
        reviewRepository.delete(review);

        // Recalculate executor rating
        recalculateExecutorRating(executorId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        return mapToResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewByOrderId(Long orderId) {
        Review review = reviewRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "orderId", orderId));
        return mapToResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getExecutorReviews(Long executorId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByExecutorIdAndIsVisibleTrueOrderByCreatedAtDesc(executorId, pageable);

        List<ReviewResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
    }

    @Override
    public boolean hasReview(Long orderId) {
        return reviewRepository.existsByOrderId(orderId);
    }

    @Override
    @Transactional
    public void recalculateExecutorRating(Long executorId) {
        ExecutorProfile profile = executorProfileRepository.findById(executorId).orElse(null);
        if (profile == null) return;

        Double avgRating = reviewRepository.calculateAverageRating(executorId);
        long reviewCount = reviewRepository.countByExecutorId(executorId);

        if (avgRating != null) {
            profile.setRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP));
        } else {
            profile.setRating(BigDecimal.ZERO);
        }
        profile.setReviewCount((int) reviewCount);

        executorProfileRepository.save(profile);
    }

    private ReviewResponse mapToResponse(Review review) {
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
