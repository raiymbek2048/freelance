package kg.freelance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kg.freelance.dto.request.ReviewRequest;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.dto.response.ReviewResponse;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ForbiddenException;
import kg.freelance.exception.GlobalExceptionHandler;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewController Tests")
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        userPrincipal = UserPrincipal.builder()
                .id(1L).email("test@example.com").fullName("Test User")
                .role(UserRole.USER).active(true).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities()));
    }

    private ReviewResponse buildReviewResponse() {
        return ReviewResponse.builder()
                .id(1L).orderId(100L).orderTitle("Test Order")
                .clientId(1L).clientName("Test User")
                .rating(5).comment("Great work!")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/reviews/orders/{orderId}")
    class CreateReviewTests {

        @Test
        @DisplayName("Should create review successfully")
        void shouldCreateReviewSuccessfully() throws Exception {
            ReviewRequest request = new ReviewRequest();
            request.setRating(5);
            request.setComment("Excellent!");

            when(reviewService.createReview(eq(1L), eq(100L), any(ReviewRequest.class)))
                    .thenReturn(buildReviewResponse());

            mockMvc.perform(post("/api/v1/reviews/orders/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.rating").value(5));
        }

        @Test
        @DisplayName("Should return 400 when order not completed")
        void shouldReturn400WhenOrderNotCompleted() throws Exception {
            ReviewRequest request = new ReviewRequest();
            request.setRating(5);

            when(reviewService.createReview(eq(1L), eq(100L), any(ReviewRequest.class)))
                    .thenThrow(new BadRequestException("Can only review completed orders"));

            mockMvc.perform(post("/api/v1/reviews/orders/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when not the client")
        void shouldReturn403WhenNotTheClient() throws Exception {
            ReviewRequest request = new ReviewRequest();
            request.setRating(5);

            when(reviewService.createReview(eq(1L), eq(100L), any(ReviewRequest.class)))
                    .thenThrow(new ForbiddenException("Only the client can leave a review"));

            mockMvc.perform(post("/api/v1/reviews/orders/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reviews/{id}")
    class GetReviewTests {

        @Test
        @DisplayName("Should return review by ID")
        void shouldReturnReviewById() throws Exception {
            when(reviewService.getReviewById(1L)).thenReturn(buildReviewResponse());

            mockMvc.perform(get("/api/v1/reviews/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.rating").value(5));
        }

        @Test
        @DisplayName("Should return 404 when review not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(reviewService.getReviewById(999L))
                    .thenThrow(new ResourceNotFoundException("Review", "id", 999L));

            mockMvc.perform(get("/api/v1/reviews/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reviews/orders/{orderId}")
    class GetReviewByOrderTests {

        @Test
        @DisplayName("Should return review by order ID")
        void shouldReturnReviewByOrderId() throws Exception {
            when(reviewService.getReviewByOrderId(100L)).thenReturn(buildReviewResponse());

            mockMvc.perform(get("/api/v1/reviews/orders/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(100));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reviews/executors/{executorId}")
    class GetExecutorReviewsTests {

        @Test
        @DisplayName("Should return executor reviews with pagination")
        void shouldReturnExecutorReviewsWithPagination() throws Exception {
            PageResponse<ReviewResponse> pageResponse = PageResponse.<ReviewResponse>builder()
                    .content(List.of(buildReviewResponse()))
                    .page(0).size(10).totalElements(1).totalPages(1)
                    .first(true).last(true).build();

            when(reviewService.getExecutorReviews(eq(2L), any())).thenReturn(pageResponse);

            mockMvc.perform(get("/api/v1/reviews/executors/2")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].rating").value(5))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/reviews/{id}")
    class UpdateReviewTests {

        @Test
        @DisplayName("Should update review successfully")
        void shouldUpdateReviewSuccessfully() throws Exception {
            ReviewRequest request = new ReviewRequest();
            request.setRating(4);
            request.setComment("Updated");

            ReviewResponse updated = ReviewResponse.builder()
                    .id(1L).rating(4).comment("Updated")
                    .orderId(100L).orderTitle("Test Order")
                    .clientId(1L).clientName("Test User")
                    .createdAt(LocalDateTime.now()).build();

            when(reviewService.updateReview(eq(1L), eq(1L), any(ReviewRequest.class))).thenReturn(updated);

            mockMvc.perform(put("/api/v1/reviews/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rating").value(4));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/reviews/{id}")
    class DeleteReviewTests {

        @Test
        @DisplayName("Should delete review successfully")
        void shouldDeleteReviewSuccessfully() throws Exception {
            doNothing().when(reviewService).deleteReview(1L, 1L);

            mockMvc.perform(delete("/api/v1/reviews/1"))
                    .andExpect(status().isNoContent());

            verify(reviewService).deleteReview(1L, 1L);
        }

        @Test
        @DisplayName("Should return 403 when not the author")
        void shouldReturn403WhenNotAuthor() throws Exception {
            doThrow(new ForbiddenException("You can only delete your own reviews"))
                    .when(reviewService).deleteReview(1L, 1L);

            mockMvc.perform(delete("/api/v1/reviews/1"))
                    .andExpect(status().isForbidden());
        }
    }
}
