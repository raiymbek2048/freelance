package kg.freelance.service;

import kg.freelance.dto.request.ExecutorProfileRequest;
import kg.freelance.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ExecutorService {

    PageResponse<ExecutorListResponse> getExecutors(
            Long categoryId,
            BigDecimal minRating,
            Boolean availableOnly,
            String search,
            Pageable pageable
    );

    ExecutorResponse getExecutorById(Long id);

    ExecutorResponse createOrUpdateProfile(Long userId, ExecutorProfileRequest request);

    ExecutorResponse updateCategories(Long userId, List<Long> categoryIds);

    void updateAvailability(Long userId, boolean available);

    PageResponse<ReviewResponse> getExecutorReviews(Long executorId, Pageable pageable);

    List<PortfolioResponse> getExecutorPortfolio(Long executorId);

    boolean hasExecutorProfile(Long userId);
}
