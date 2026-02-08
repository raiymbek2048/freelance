package kg.freelance.service.impl;

import kg.freelance.dto.request.ReviewRequest;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.dto.response.ReviewResponse;
import kg.freelance.entity.*;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ForbiddenException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.ExecutorProfileRepository;
import kg.freelance.repository.OrderRepository;
import kg.freelance.repository.ReviewRepository;
import kg.freelance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Tests")
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExecutorProfileRepository executorProfileRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User client;
    private User executor;
    private Order order;
    private Review review;
    private ExecutorProfile executorProfile;

    @BeforeEach
    void setUp() {
        client = User.builder()
                .id(1L)
                .email("client@example.com")
                .fullName("Client User")
                .role(UserRole.USER)
                .build();

        executorProfile = new ExecutorProfile();
        executorProfile.setId(2L);
        executorProfile.setRating(BigDecimal.valueOf(4.5));
        executorProfile.setReviewCount(5);

        executor = User.builder()
                .id(2L)
                .email("executor@example.com")
                .fullName("Executor User")
                .role(UserRole.USER)
                .executorProfile(executorProfile)
                .build();
        executorProfile.setUser(executor);

        order = Order.builder()
                .id(1L)
                .title("Test Order")
                .description("Test description")
                .client(client)
                .executor(executor)
                .status(OrderStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        review = Review.builder()
                .id(1L)
                .order(order)
                .client(client)
                .executor(executor)
                .rating(5)
                .comment("Great work!")
                .isModerated(false)
                .isVisible(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Create Review Tests")
    class CreateReviewTests {

        @Test
        @DisplayName("Should create review successfully")
        void shouldCreateReviewSuccessfully() {
            // Given
            ReviewRequest request = new ReviewRequest();
            request.setRating(5);
            request.setComment("Excellent work!");

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(reviewRepository.existsByOrderId(1L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(client));
            when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
                Review r = inv.getArgument(0);
                r.setId(1L);
                r.setCreatedAt(LocalDateTime.now());
                return r;
            });
            when(executorProfileRepository.findById(2L)).thenReturn(Optional.of(executorProfile));
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(5.0);
            when(reviewRepository.countByExecutorId(2L)).thenReturn(1L);

            // When
            ReviewResponse result = reviewService.createReview(1L, 1L, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRating()).isEqualTo(5);
            assertThat(result.getComment()).isEqualTo("Excellent work!");
            verify(reviewRepository).save(any(Review.class));
            verify(executorProfileRepository).save(any(ExecutorProfile.class));
        }

        @Test
        @DisplayName("Should throw exception when not the client")
        void shouldThrowExceptionWhenNotClient() {
            // Given
            ReviewRequest request = new ReviewRequest();
            request.setRating(5);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            // When/Then - User 999 is not the client
            assertThatThrownBy(() -> reviewService.createReview(999L, 1L, request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Only the client can leave a review");
        }

        @Test
        @DisplayName("Should throw exception when order not completed")
        void shouldThrowExceptionWhenOrderNotCompleted() {
            // Given
            order.setStatus(OrderStatus.IN_PROGRESS);
            ReviewRequest request = new ReviewRequest();
            request.setRating(5);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            // When/Then
            assertThatThrownBy(() -> reviewService.createReview(1L, 1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Can only review completed orders");
        }

        @Test
        @DisplayName("Should throw exception when review already exists")
        void shouldThrowExceptionWhenReviewAlreadyExists() {
            // Given
            ReviewRequest request = new ReviewRequest();
            request.setRating(5);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(reviewRepository.existsByOrderId(1L)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> reviewService.createReview(1L, 1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Review already exists for this order");
        }

        @Test
        @DisplayName("Should throw exception when order has no executor")
        void shouldThrowExceptionWhenOrderHasNoExecutor() {
            // Given
            order.setExecutor(null);
            ReviewRequest request = new ReviewRequest();
            request.setRating(5);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(reviewRepository.existsByOrderId(1L)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> reviewService.createReview(1L, 1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Order has no executor");
        }
    }

    @Nested
    @DisplayName("Update Review Tests")
    class UpdateReviewTests {

        @Test
        @DisplayName("Should update review successfully")
        void shouldUpdateReviewSuccessfully() {
            // Given
            ReviewRequest request = new ReviewRequest();
            request.setRating(4);
            request.setComment("Updated comment");

            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any(Review.class))).thenReturn(review);
            when(executorProfileRepository.findById(2L)).thenReturn(Optional.of(executorProfile));
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(4.0);
            when(reviewRepository.countByExecutorId(2L)).thenReturn(1L);

            // When
            ReviewResponse result = reviewService.updateReview(1L, 1L, request);

            // Then
            assertThat(result).isNotNull();
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("Should throw exception when not the review author")
        void shouldThrowExceptionWhenNotReviewAuthor() {
            // Given
            ReviewRequest request = new ReviewRequest();
            request.setRating(4);

            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

            // When/Then
            assertThatThrownBy(() -> reviewService.updateReview(999L, 1L, request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You can only update your own reviews");
        }
    }

    @Nested
    @DisplayName("Delete Review Tests")
    class DeleteReviewTests {

        @Test
        @DisplayName("Should delete review successfully")
        void shouldDeleteReviewSuccessfully() {
            // Given
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
            when(executorProfileRepository.findById(2L)).thenReturn(Optional.of(executorProfile));
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(null);
            when(reviewRepository.countByExecutorId(2L)).thenReturn(0L);

            // When
            reviewService.deleteReview(1L, 1L);

            // Then
            verify(reviewRepository).delete(review);
            verify(executorProfileRepository).save(any(ExecutorProfile.class));
        }

        @Test
        @DisplayName("Should throw exception when not the review author")
        void shouldThrowExceptionWhenNotAuthor() {
            // Given
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

            // When/Then
            assertThatThrownBy(() -> reviewService.deleteReview(999L, 1L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You can only delete your own reviews");
        }
    }

    @Nested
    @DisplayName("Get Review Tests")
    class GetReviewTests {

        @Test
        @DisplayName("Should get review by ID")
        void shouldGetReviewById() {
            // Given
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

            // When
            ReviewResponse result = reviewService.getReviewById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getRating()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should throw exception when review not found")
        void shouldThrowExceptionWhenReviewNotFound() {
            // Given
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> reviewService.getReviewById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should get review by order ID")
        void shouldGetReviewByOrderId() {
            // Given
            when(reviewRepository.findByOrderId(1L)).thenReturn(Optional.of(review));

            // When
            ReviewResponse result = reviewService.getReviewByOrderId(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should get executor reviews")
        void shouldGetExecutorReviews() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> page = new PageImpl<>(List.of(review), pageable, 1);

            when(reviewRepository.findByExecutorIdAndIsVisibleTrueOrderByCreatedAtDesc(2L, pageable))
                    .thenReturn(page);

            // When
            PageResponse<ReviewResponse> result = reviewService.getExecutorReviews(2L, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Has Review Tests")
    class HasReviewTests {

        @Test
        @DisplayName("Should check if review exists")
        void shouldCheckIfReviewExists() {
            // Given
            when(reviewRepository.existsByOrderId(1L)).thenReturn(true);
            when(reviewRepository.existsByOrderId(2L)).thenReturn(false);

            // When/Then
            assertThat(reviewService.hasReview(1L)).isTrue();
            assertThat(reviewService.hasReview(2L)).isFalse();
        }
    }

    @Nested
    @DisplayName("Recalculate Rating Tests")
    class RecalculateRatingTests {

        @Test
        @DisplayName("Should recalculate executor rating")
        void shouldRecalculateExecutorRating() {
            // Given
            when(executorProfileRepository.findById(2L)).thenReturn(Optional.of(executorProfile));
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(4.5);
            when(reviewRepository.countByExecutorId(2L)).thenReturn(10L);

            // When
            reviewService.recalculateExecutorRating(2L);

            // Then
            verify(executorProfileRepository).save(argThat(profile ->
                    profile.getRating().compareTo(BigDecimal.valueOf(4.50)) == 0 &&
                    profile.getReviewCount() == 10
            ));
        }

        @Test
        @DisplayName("Should set zero rating when no reviews")
        void shouldSetZeroRatingWhenNoReviews() {
            // Given
            when(executorProfileRepository.findById(2L)).thenReturn(Optional.of(executorProfile));
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(null);
            when(reviewRepository.countByExecutorId(2L)).thenReturn(0L);

            // When
            reviewService.recalculateExecutorRating(2L);

            // Then
            verify(executorProfileRepository).save(argThat(profile ->
                    profile.getRating().compareTo(BigDecimal.ZERO) == 0 &&
                    profile.getReviewCount() == 0
            ));
        }
    }
}
