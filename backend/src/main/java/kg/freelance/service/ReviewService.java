package kg.freelance.service;

import kg.freelance.dto.request.ReviewRequest;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.dto.response.ReviewResponse;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponse createReview(Long clientId, Long orderId, ReviewRequest request);

    ReviewResponse updateReview(Long clientId, Long reviewId, ReviewRequest request);

    void deleteReview(Long clientId, Long reviewId);

    ReviewResponse getReviewById(Long reviewId);

    ReviewResponse getReviewByOrderId(Long orderId);

    PageResponse<ReviewResponse> getExecutorReviews(Long executorId, Pageable pageable);

    boolean hasReview(Long orderId);

    void recalculateExecutorRating(Long executorId);
}
