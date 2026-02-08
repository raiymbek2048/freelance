package kg.freelance.service.impl;

import kg.freelance.dto.request.ExecutorProfileRequest;
import kg.freelance.dto.response.ExecutorListResponse;
import kg.freelance.dto.response.ExecutorResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.dto.response.PortfolioResponse;
import kg.freelance.dto.response.ReviewResponse;
import kg.freelance.entity.*;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.*;
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
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutorService Tests")
class ExecutorServiceImplTest {

    @Mock
    private ExecutorProfileRepository executorProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @InjectMocks
    private ExecutorServiceImpl executorService;

    private User user;
    private ExecutorProfile executorProfile;
    private Category category;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("executor@example.com")
                .fullName("Executor User")
                .avatarUrl("/avatars/1.jpg")
                .whatsappLink("https://wa.me/996555123456")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .build();

        category = Category.builder()
                .id(10L)
                .name("Web Development")
                .slug("web-development")
                .active(true)
                .build();

        executorProfile = new ExecutorProfile();
        executorProfile.setId(1L);
        executorProfile.setUser(user);
        executorProfile.setBio("Experienced developer");
        executorProfile.setSpecialization("Full Stack Developer");
        executorProfile.setTotalOrders(20);
        executorProfile.setCompletedOrders(18);
        executorProfile.setDisputedOrders(1);
        executorProfile.setAvgCompletionDays(5.0);
        executorProfile.setRating(BigDecimal.valueOf(4.75));
        executorProfile.setReviewCount(15);
        executorProfile.setAvailableForWork(true);
        executorProfile.setLastActiveAt(LocalDateTime.now());
        executorProfile.setCategories(new HashSet<>(Set.of(category)));
    }

    @Nested
    @DisplayName("Get Executors Tests")
    class GetExecutorsTests {

        @Test
        @DisplayName("Should return paginated list of executors")
        void shouldReturnPaginatedListOfExecutors() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ExecutorProfile> page = new PageImpl<>(List.of(executorProfile), pageable, 1);

            when(executorProfileRepository.findExecutors(null, null, false, null, pageable))
                    .thenReturn(page);

            // When
            PageResponse<ExecutorListResponse> result = executorService.getExecutors(
                    null, null, null, null, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            ExecutorListResponse executor = result.getContent().get(0);
            assertThat(executor.getId()).isEqualTo(1L);
            assertThat(executor.getFullName()).isEqualTo("Executor User");
            assertThat(executor.getRating()).isEqualTo(BigDecimal.valueOf(4.75));
            assertThat(executor.getCategories()).hasSize(1);
        }

        @Test
        @DisplayName("Should filter by category ID")
        void shouldFilterByCategoryId() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ExecutorProfile> page = new PageImpl<>(List.of(executorProfile), pageable, 1);

            when(executorProfileRepository.findExecutors(eq(10L), isNull(), eq(false), isNull(), eq(pageable)))
                    .thenReturn(page);

            // When
            PageResponse<ExecutorListResponse> result = executorService.getExecutors(
                    10L, null, null, null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should filter available only when true")
        void shouldFilterAvailableOnlyWhenTrue() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ExecutorProfile> page = new PageImpl<>(List.of(executorProfile), pageable, 1);

            when(executorProfileRepository.findExecutors(isNull(), isNull(), eq(true), isNull(), eq(pageable)))
                    .thenReturn(page);

            // When
            PageResponse<ExecutorListResponse> result = executorService.getExecutors(
                    null, null, true, null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty page when no executors match")
        void shouldReturnEmptyPageWhenNoExecutorsMatch() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ExecutorProfile> page = new PageImpl<>(List.of(), pageable, 0);

            when(executorProfileRepository.findExecutors(any(), any(), anyBoolean(), any(), any()))
                    .thenReturn(page);

            // When
            PageResponse<ExecutorListResponse> result = executorService.getExecutors(
                    null, null, null, null, pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("Get Executor By ID Tests")
    class GetExecutorByIdTests {

        @Test
        @DisplayName("Should return executor by ID")
        void shouldReturnExecutorById() {
            // Given
            when(executorProfileRepository.findById(1L)).thenReturn(Optional.of(executorProfile));

            // When
            ExecutorResponse result = executorService.getExecutorById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFullName()).isEqualTo("Executor User");
            assertThat(result.getBio()).isEqualTo("Experienced developer");
            assertThat(result.getSpecialization()).isEqualTo("Full Stack Developer");
            assertThat(result.getTotalOrders()).isEqualTo(20);
            assertThat(result.getCompletedOrders()).isEqualTo(18);
            assertThat(result.getRating()).isEqualTo(BigDecimal.valueOf(4.75));
            assertThat(result.getAvailableForWork()).isTrue();
            assertThat(result.getWhatsappLink()).isEqualTo("https://wa.me/996555123456");
            assertThat(result.getCategories()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw exception when executor not found")
        void shouldThrowExceptionWhenExecutorNotFound() {
            // Given
            when(executorProfileRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> executorService.getExecutorById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Create Or Update Profile Tests")
    class CreateOrUpdateProfileTests {

        @Test
        @DisplayName("Should create new executor profile")
        void shouldCreateNewExecutorProfile() {
            // Given
            ExecutorProfileRequest request = new ExecutorProfileRequest();
            request.setBio("New bio");
            request.setSpecialization("Designer");
            request.setAvailableForWork(true);
            request.setCategoryIds(Set.of(10L));

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(executorProfileRepository.findById(1L)).thenReturn(Optional.empty());
            when(categoryRepository.findAllById(Set.of(10L))).thenReturn(List.of(category));
            when(executorProfileRepository.save(any(ExecutorProfile.class))).thenAnswer(inv -> {
                ExecutorProfile p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });

            // When
            ExecutorResponse result = executorService.createOrUpdateProfile(1L, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBio()).isEqualTo("New bio");
            assertThat(result.getSpecialization()).isEqualTo("Designer");
            verify(executorProfileRepository).save(any(ExecutorProfile.class));
        }

        @Test
        @DisplayName("Should update existing executor profile")
        void shouldUpdateExistingExecutorProfile() {
            // Given
            ExecutorProfileRequest request = new ExecutorProfileRequest();
            request.setBio("Updated bio");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(executorProfileRepository.findById(1L)).thenReturn(Optional.of(executorProfile));
            when(executorProfileRepository.save(any(ExecutorProfile.class))).thenReturn(executorProfile);

            // When
            ExecutorResponse result = executorService.createOrUpdateProfile(1L, request);

            // Then
            assertThat(result).isNotNull();
            verify(executorProfileRepository).save(any(ExecutorProfile.class));
        }

        @Test
        @DisplayName("Should not update null fields")
        void shouldNotUpdateNullFields() {
            // Given
            ExecutorProfileRequest request = new ExecutorProfileRequest();
            // All fields null

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(executorProfileRepository.findById(1L)).thenReturn(Optional.of(executorProfile));
            when(executorProfileRepository.save(any(ExecutorProfile.class))).thenReturn(executorProfile);

            // When
            executorService.createOrUpdateProfile(1L, request);

            // Then
            assertThat(executorProfile.getBio()).isEqualTo("Experienced developer");
            assertThat(executorProfile.getSpecialization()).isEqualTo("Full Stack Developer");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            ExecutorProfileRequest request = new ExecutorProfileRequest();
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> executorService.createOrUpdateProfile(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Categories Tests")
    class UpdateCategoriesTests {

        @Test
        @DisplayName("Should update executor categories")
        void shouldUpdateExecutorCategories() {
            // Given
            Category newCategory = Category.builder()
                    .id(20L)
                    .name("Design")
                    .slug("design")
                    .build();

            when(executorProfileRepository.findById(1L)).thenReturn(Optional.of(executorProfile));
            when(categoryRepository.findAllById(List.of(20L))).thenReturn(List.of(newCategory));
            when(executorProfileRepository.save(any(ExecutorProfile.class))).thenReturn(executorProfile);

            // When
            ExecutorResponse result = executorService.updateCategories(1L, List.of(20L));

            // Then
            assertThat(result).isNotNull();
            verify(executorProfileRepository).save(argThat(profile ->
                    profile.getCategories().contains(newCategory)));
        }

        @Test
        @DisplayName("Should throw exception when profile not found")
        void shouldThrowExceptionWhenProfileNotFound() {
            // Given
            when(executorProfileRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> executorService.updateCategories(999L, List.of(10L)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Availability Tests")
    class UpdateAvailabilityTests {

        @Test
        @DisplayName("Should update availability to false")
        void shouldUpdateAvailabilityToFalse() {
            // Given
            when(executorProfileRepository.findById(1L)).thenReturn(Optional.of(executorProfile));

            // When
            executorService.updateAvailability(1L, false);

            // Then
            assertThat(executorProfile.getAvailableForWork()).isFalse();
            assertThat(executorProfile.getLastActiveAt()).isNotNull();
            verify(executorProfileRepository).save(executorProfile);
        }

        @Test
        @DisplayName("Should update availability to true")
        void shouldUpdateAvailabilityToTrue() {
            // Given
            executorProfile.setAvailableForWork(false);
            when(executorProfileRepository.findById(1L)).thenReturn(Optional.of(executorProfile));

            // When
            executorService.updateAvailability(1L, true);

            // Then
            assertThat(executorProfile.getAvailableForWork()).isTrue();
            verify(executorProfileRepository).save(executorProfile);
        }

        @Test
        @DisplayName("Should throw exception when profile not found")
        void shouldThrowExceptionWhenProfileNotFoundForAvailability() {
            // Given
            when(executorProfileRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> executorService.updateAvailability(999L, true))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Executor Reviews Tests")
    class GetExecutorReviewsTests {

        @Test
        @DisplayName("Should return executor reviews")
        void shouldReturnExecutorReviews() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            User client = User.builder()
                    .id(5L)
                    .fullName("Client User")
                    .avatarUrl("/avatars/5.jpg")
                    .build();
            Order order = Order.builder()
                    .id(100L)
                    .title("Test Order")
                    .build();
            Review review = Review.builder()
                    .id(1L)
                    .order(order)
                    .client(client)
                    .executor(user)
                    .rating(5)
                    .comment("Great work!")
                    .createdAt(LocalDateTime.now())
                    .build();
            Page<Review> page = new PageImpl<>(List.of(review), pageable, 1);

            when(executorProfileRepository.existsById(1L)).thenReturn(true);
            when(reviewRepository.findByExecutorIdAndIsVisibleTrueOrderByCreatedAtDesc(1L, pageable))
                    .thenReturn(page);

            // When
            PageResponse<ReviewResponse> result = executorService.getExecutorReviews(1L, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            ReviewResponse reviewResponse = result.getContent().get(0);
            assertThat(reviewResponse.getRating()).isEqualTo(5);
            assertThat(reviewResponse.getComment()).isEqualTo("Great work!");
            assertThat(reviewResponse.getClientName()).isEqualTo("Client User");
        }

        @Test
        @DisplayName("Should throw exception when executor not found")
        void shouldThrowExceptionWhenExecutorNotFoundForReviews() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            when(executorProfileRepository.existsById(999L)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> executorService.getExecutorReviews(999L, pageable))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Executor Portfolio Tests")
    class GetExecutorPortfolioTests {

        @Test
        @DisplayName("Should return executor portfolio items")
        void shouldReturnExecutorPortfolioItems() {
            // Given
            Portfolio portfolio = Portfolio.builder()
                    .id(1L)
                    .title("Project A")
                    .description("A nice project")
                    .category(category)
                    .images(List.of("/images/1.jpg", "/images/2.jpg"))
                    .externalLink("https://example.com")
                    .sortOrder(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(executorProfileRepository.existsById(1L)).thenReturn(true);
            when(portfolioRepository.findByExecutorIdOrderBySortOrder(1L))
                    .thenReturn(List.of(portfolio));

            // When
            List<PortfolioResponse> result = executorService.getExecutorPortfolio(1L);

            // Then
            assertThat(result).hasSize(1);
            PortfolioResponse item = result.get(0);
            assertThat(item.getTitle()).isEqualTo("Project A");
            assertThat(item.getCategoryName()).isEqualTo("Web Development");
            assertThat(item.getImages()).hasSize(2);
        }

        @Test
        @DisplayName("Should return portfolio item with null category")
        void shouldReturnPortfolioItemWithNullCategory() {
            // Given
            Portfolio portfolio = Portfolio.builder()
                    .id(1L)
                    .title("Project B")
                    .category(null)
                    .images(List.of())
                    .sortOrder(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(executorProfileRepository.existsById(1L)).thenReturn(true);
            when(portfolioRepository.findByExecutorIdOrderBySortOrder(1L))
                    .thenReturn(List.of(portfolio));

            // When
            List<PortfolioResponse> result = executorService.getExecutorPortfolio(1L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategoryId()).isNull();
            assertThat(result.get(0).getCategoryName()).isNull();
        }

        @Test
        @DisplayName("Should throw exception when executor not found")
        void shouldThrowExceptionWhenExecutorNotFoundForPortfolio() {
            // Given
            when(executorProfileRepository.existsById(999L)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> executorService.getExecutorPortfolio(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Has Executor Profile Tests")
    class HasExecutorProfileTests {

        @Test
        @DisplayName("Should return true when profile exists")
        void shouldReturnTrueWhenProfileExists() {
            // Given
            when(executorProfileRepository.existsById(1L)).thenReturn(true);

            // When/Then
            assertThat(executorService.hasExecutorProfile(1L)).isTrue();
        }

        @Test
        @DisplayName("Should return false when profile does not exist")
        void shouldReturnFalseWhenProfileDoesNotExist() {
            // Given
            when(executorProfileRepository.existsById(999L)).thenReturn(false);

            // When/Then
            assertThat(executorService.hasExecutorProfile(999L)).isFalse();
        }
    }
}
