package kg.freelance.service.impl;

import kg.freelance.dto.request.CategoryCreateRequest;
import kg.freelance.dto.response.*;
import kg.freelance.entity.*;
import kg.freelance.repository.DisputeRepository;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.entity.enums.ProfileVisibility;
import kg.freelance.entity.enums.SubscriptionStatus;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.*;
import kg.freelance.service.ReviewService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Tests")
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ExecutorProfileRepository executorProfileRepository;

    @Mock
    private ReviewService reviewService;

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private SubscriptionSettingsRepository subscriptionSettingsRepository;

    @Mock
    private OrderResponseRepository orderResponseRepository;

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private kg.freelance.service.DisputeService disputeService;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User user;
    private User executor;
    private Order order;
    private Category category;
    private Review review;
    private ExecutorProfile executorProfile;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Test User")
                .role(UserRole.USER)
                .profileVisibility(ProfileVisibility.PUBLIC)
                .hideFromExecutorList(false)
                .emailVerified(false)
                .phoneVerified(false)
                .executorVerified(false)
                .active(true)
                .createdAt(LocalDateTime.now())
                .ordersAsClient(new ArrayList<>())
                .ordersAsExecutor(new ArrayList<>())
                .build();

        executorProfile = new ExecutorProfile();
        executorProfile.setId(2L);
        executorProfile.setTotalOrders(5);
        executorProfile.setCompletedOrders(3);
        executorProfile.setRating(BigDecimal.valueOf(4.5));
        executorProfile.setReviewCount(3);

        executor = User.builder()
                .id(2L)
                .email("executor@example.com")
                .fullName("Test Executor")
                .role(UserRole.USER)
                .executorVerified(true)
                .active(true)
                .executorProfile(executorProfile)
                .ordersAsClient(new ArrayList<>())
                .ordersAsExecutor(new ArrayList<>())
                .build();
        executorProfile.setUser(executor);

        category = Category.builder()
                .id(1L)
                .name("Web Development")
                .slug("web-development")
                .active(true)
                .build();

        order = Order.builder()
                .id(1L)
                .title("Test Order")
                .description("Test description")
                .client(user)
                .executor(executor)
                .category(category)
                .status(OrderStatus.IN_PROGRESS)
                .budgetMin(BigDecimal.valueOf(1000))
                .budgetMax(BigDecimal.valueOf(5000))
                .isPublic(true)
                .responseCount(0)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        review = Review.builder()
                .id(1L)
                .order(order)
                .client(user)
                .executor(executor)
                .rating(5)
                .comment("Great work!")
                .isModerated(false)
                .isVisible(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("User Management Tests")
    class UserManagementTests {

        @Test
        @DisplayName("Should get user by ID")
        void shouldGetUserById() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // When
            AdminUserResponse result = adminService.getUserById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adminService.getUserById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should block user")
        void shouldBlockUser() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // When
            adminService.blockUser(1L);

            // Then
            verify(userRepository).save(argThat(u -> !u.getActive()));
        }

        @Test
        @DisplayName("Should unblock user")
        void shouldUnblockUser() {
            // Given
            user.setActive(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // When
            adminService.unblockUser(1L);

            // Then
            verify(userRepository).save(argThat(User::getActive));
        }

        @Test
        @DisplayName("Should change user role")
        void shouldChangeUserRole() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // When
            adminService.changeUserRole(1L, UserRole.ADMIN);

            // Then
            verify(userRepository).save(argThat(u -> u.getRole() == UserRole.ADMIN));
        }

        @Test
        @DisplayName("Should get all users with pagination")
        void shouldGetAllUsersWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> page = new PageImpl<>(List.of(user), pageable, 1);

            when(userRepository.findAll(pageable)).thenReturn(page);

            // When
            PageResponse<AdminUserResponse> result = adminService.getAllUsers(null, null, null, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Order Management Tests")
    class OrderManagementTests {

        @Test
        @DisplayName("Should get order by ID")
        void shouldGetOrderById() {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            // When
            AdminOrderResponse result = adminService.getOrderById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Test Order");
        }

        @Test
        @DisplayName("Should resolve dispute in favor of client")
        void shouldResolveDisputeInFavorOfClient() {
            // Given
            order.setStatus(OrderStatus.DISPUTED);
            Dispute dispute = Dispute.builder().id(10L).order(order).build();
            when(disputeRepository.findByOrderId(1L)).thenReturn(Optional.of(dispute));
            when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(
                    User.builder().id(99L).role(UserRole.ADMIN).build()
            ));

            // When
            adminService.resolveDispute(1L, true, "Client was right");

            // Then
            verify(disputeService).resolveDispute(eq(10L), eq(99L), any());
        }

        @Test
        @DisplayName("Should resolve dispute in favor of executor")
        void shouldResolveDisputeInFavorOfExecutor() {
            // Given
            order.setStatus(OrderStatus.DISPUTED);
            Dispute dispute = Dispute.builder().id(10L).order(order).build();
            when(disputeRepository.findByOrderId(1L)).thenReturn(Optional.of(dispute));
            when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(
                    User.builder().id(99L).role(UserRole.ADMIN).build()
            ));

            // When
            adminService.resolveDispute(1L, false, "Executor was right");

            // Then
            verify(disputeService).resolveDispute(eq(10L), eq(99L), any());
        }

        @Test
        @DisplayName("Should throw exception when no dispute found for order")
        void shouldThrowExceptionWhenOrderNotInDispute() {
            // Given
            when(disputeRepository.findByOrderId(1L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adminService.resolveDispute(1L, true, "Resolution"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("No dispute found");
        }

        @Test
        @DisplayName("Should delete order")
        void shouldDeleteOrder() {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            // When
            adminService.deleteOrder(1L);

            // Then
            verify(orderRepository).delete(order);
        }

        @Test
        @DisplayName("Should get disputed orders")
        void shouldGetDisputedOrders() {
            // Given
            order.setStatus(OrderStatus.DISPUTED);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);

            when(orderRepository.findAll(pageable)).thenReturn(page);

            // When
            PageResponse<AdminOrderResponse> result = adminService.getDisputedOrders(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Category Management Tests")
    class CategoryManagementTests {

        @Test
        @DisplayName("Should create category")
        void shouldCreateCategory() {
            // Given
            CategoryCreateRequest request = new CategoryCreateRequest();
            request.setName("New Category");
            request.setSlug("new-category");
            request.setDescription("Description");

            when(categoryRepository.existsBySlug("new-category")).thenReturn(false);
            when(categoryRepository.existsByName("New Category")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(2L);
                return c;
            });

            // When
            CategoryResponse result = adminService.createCategory(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("New Category");
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should throw exception when slug already exists")
        void shouldThrowExceptionWhenSlugExists() {
            // Given
            CategoryCreateRequest request = new CategoryCreateRequest();
            request.setName("New Category");
            request.setSlug("existing-slug");

            when(categoryRepository.existsBySlug("existing-slug")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> adminService.createCategory(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Category with this slug already exists");
        }

        @Test
        @DisplayName("Should throw exception when name already exists")
        void shouldThrowExceptionWhenNameExists() {
            // Given
            CategoryCreateRequest request = new CategoryCreateRequest();
            request.setName("Existing Category");
            request.setSlug("new-slug");

            when(categoryRepository.existsBySlug("new-slug")).thenReturn(false);
            when(categoryRepository.existsByName("Existing Category")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> adminService.createCategory(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Category with this name already exists");
        }

        @Test
        @DisplayName("Should update category")
        void shouldUpdateCategory() {
            // Given
            CategoryCreateRequest request = new CategoryCreateRequest();
            request.setName("Updated Name");
            request.setDescription("Updated description");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByName("Updated Name")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);

            // When
            CategoryResponse result = adminService.updateCategory(1L, request);

            // Then
            assertThat(result).isNotNull();
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should delete category (soft delete)")
        void shouldDeleteCategory() {
            // Given
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            // When
            adminService.deleteCategory(1L);

            // Then
            verify(categoryRepository).save(argThat(c -> !c.getActive()));
        }
    }

    @Nested
    @DisplayName("Review Management Tests")
    class ReviewManagementTests {

        @Test
        @DisplayName("Should approve review")
        void shouldApproveReview() {
            // Given
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

            // When
            adminService.approveReview(1L);

            // Then
            verify(reviewRepository).save(argThat(r -> r.getIsModerated() && r.getIsVisible()));
            verify(reviewService).recalculateExecutorRating(2L);
        }

        @Test
        @DisplayName("Should reject review")
        void shouldRejectReview() {
            // Given
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

            // When
            adminService.rejectReview(1L, "Inappropriate content");

            // Then
            verify(reviewRepository).save(argThat(r ->
                    r.getIsModerated() &&
                    !r.getIsVisible() &&
                    r.getModeratorComment().equals("Inappropriate content")
            ));
            verify(reviewService).recalculateExecutorRating(2L);
        }

        @Test
        @DisplayName("Should delete review")
        void shouldDeleteReview() {
            // Given
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

            // When
            adminService.deleteReview(1L);

            // Then
            verify(reviewRepository).delete(review);
            verify(reviewService).recalculateExecutorRating(2L);
        }

        @Test
        @DisplayName("Should throw exception when review not found")
        void shouldThrowExceptionWhenReviewNotFound() {
            // Given
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adminService.approveReview(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Overview Stats Tests")
    class OverviewStatsTests {

        @Test
        @DisplayName("Should return overview stats using optimized queries")
        void shouldReturnOverviewStats() {
            when(userRepository.count()).thenReturn(100L);
            when(userRepository.countByActiveTrue()).thenReturn(90L);
            when(executorProfileRepository.count()).thenReturn(30L);
            when(orderRepository.count()).thenReturn(50L);
            when(reviewRepository.count()).thenReturn(20L);

            when(userRepository.countByCreatedAtAfter(any())).thenReturn(5L);

            when(orderRepository.countByStatus(OrderStatus.NEW)).thenReturn(10L);
            when(orderRepository.countByStatus(OrderStatus.IN_PROGRESS)).thenReturn(8L);
            when(orderRepository.countByStatus(OrderStatus.REVISION)).thenReturn(2L);
            when(orderRepository.countByStatus(OrderStatus.COMPLETED)).thenReturn(25L);
            when(orderRepository.countByStatus(OrderStatus.DISPUTED)).thenReturn(3L);
            when(orderRepository.countByStatus(OrderStatus.CANCELLED)).thenReturn(4L);

            when(orderRepository.sumAgreedPrice()).thenReturn(BigDecimal.valueOf(100000));
            when(orderRepository.countWithAgreedPrice()).thenReturn(20L);

            when(reviewRepository.countByIsModeratedFalse()).thenReturn(5L);
            when(reviewRepository.calculateOverallAverageRating()).thenReturn(4.2);

            when(orderRepository.countOrdersByCategory()).thenReturn(List.of(
                    new Object[]{1L, "Web Dev", 15L},
                    new Object[]{2L, "Mobile", 10L}
            ));
            when(categoryRepository.count()).thenReturn(5L);

            AdminStatsResponse result = adminService.getOverviewStats();

            assertThat(result.getTotalUsers()).isEqualTo(100L);
            assertThat(result.getActiveUsers()).isEqualTo(90L);
            assertThat(result.getExecutors()).isEqualTo(30L);
            assertThat(result.getTotalOrders()).isEqualTo(50L);
            assertThat(result.getNewOrders()).isEqualTo(10L);
            assertThat(result.getCompletedOrders()).isEqualTo(25L);
            assertThat(result.getTotalOrdersValue()).isEqualByComparingTo(BigDecimal.valueOf(100000));
            assertThat(result.getAverageOrderValue()).isEqualByComparingTo(BigDecimal.valueOf(5000));
            assertThat(result.getPendingModeration()).isEqualTo(5L);
            assertThat(result.getAverageRating()).isEqualTo(4.2);
            assertThat(result.getTotalCategories()).isEqualTo(5L);
            assertThat(result.getTopCategories()).hasSize(2);
            assertThat(result.getTopCategories().get(0).getCategoryName()).isEqualTo("Web Dev");

            // Verify no findAll() calls
            verify(userRepository, never()).findAll();
            verify(orderRepository, never()).findAll();
            verify(reviewRepository, never()).findAll();
        }

        @Test
        @DisplayName("Should handle null average rating")
        void shouldHandleNullAverageRating() {
            when(userRepository.count()).thenReturn(0L);
            when(userRepository.countByActiveTrue()).thenReturn(0L);
            when(executorProfileRepository.count()).thenReturn(0L);
            when(orderRepository.count()).thenReturn(0L);
            when(reviewRepository.count()).thenReturn(0L);
            when(userRepository.countByCreatedAtAfter(any())).thenReturn(0L);
            when(orderRepository.countByStatus(any())).thenReturn(0L);
            when(orderRepository.sumAgreedPrice()).thenReturn(BigDecimal.ZERO);
            when(orderRepository.countWithAgreedPrice()).thenReturn(0L);
            when(reviewRepository.countByIsModeratedFalse()).thenReturn(0L);
            when(reviewRepository.calculateOverallAverageRating()).thenReturn(null);
            when(orderRepository.countOrdersByCategory()).thenReturn(List.of());
            when(categoryRepository.count()).thenReturn(0L);

            AdminStatsResponse result = adminService.getOverviewStats();

            assertThat(result.getAverageRating()).isEqualTo(0.0);
            assertThat(result.getAverageOrderValue()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Analytics Tests")
    class AnalyticsTests {

        @Test
        @DisplayName("Should return analytics using optimized queries")
        void shouldReturnAnalytics() {
            SubscriptionSettings settings = SubscriptionSettings.builder()
                    .id(1L).price(BigDecimal.valueOf(500)).trialDays(7).announcementEnabled(false).build();
            when(subscriptionSettingsRepository.getSettings()).thenReturn(settings);

            when(userRepository.countByCreatedAtBetween(any(), any())).thenReturn(2L);
            when(orderRepository.countByCreatedAtBetween(any(), any())).thenReturn(3L);
            when(orderRepository.countCompletedBetween(any(), any())).thenReturn(1L);
            when(userSubscriptionRepository.countByStatusAndCreatedAtBetween(eq(SubscriptionStatus.ACTIVE), any(), any())).thenReturn(1L);

            when(userSubscriptionRepository.count()).thenReturn(10L);
            when(userSubscriptionRepository.countCurrentlyActive(any())).thenReturn(5L);
            when(userSubscriptionRepository.countCurrentlyTrial(any())).thenReturn(2L);
            when(userSubscriptionRepository.countExpired(any())).thenReturn(3L);
            when(userSubscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE)).thenReturn(8L);

            when(userRepository.count()).thenReturn(100L);
            when(executorProfileRepository.count()).thenReturn(30L);
            when(userRepository.countVerifiedExecutors()).thenReturn(20L);
            when(orderRepository.count()).thenReturn(50L);
            when(orderRepository.countByStatus(OrderStatus.COMPLETED)).thenReturn(25L);
            when(orderResponseRepository.count()).thenReturn(200L);
            when(orderResponseRepository.countSelected()).thenReturn(40L);

            AnalyticsResponse result = adminService.getAnalytics();

            assertThat(result.getDailyStats()).hasSize(30);
            assertThat(result.getWeeklyStats()).hasSize(12);
            assertThat(result.getMonthlyStats()).hasSize(12);

            assertThat(result.getSubscriptions().getTotalSubscriptions()).isEqualTo(10L);
            assertThat(result.getSubscriptions().getActiveSubscriptions()).isEqualTo(5L);
            assertThat(result.getSubscriptions().getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(4000));

            assertThat(result.getConversions().getRegistrationToExecutorRate()).isEqualTo(30.0);
            assertThat(result.getConversions().getOrderCompletionRate()).isEqualTo(50.0);
            assertThat(result.getConversions().getResponseToSelectionRate()).isEqualTo(20.0);

            // Verify no findAll() calls — all queries are optimized
            verify(userRepository, never()).findAll();
            verify(orderRepository, never()).findAll();
            verify(userSubscriptionRepository, never()).findAll();
            verify(orderResponseRepository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("CSV Export Tests")
    class CsvExportTests {

        @Test
        @DisplayName("Should export analytics as CSV")
        void shouldExportAnalyticsAsCsv() {
            // Mock for getAnalytics()
            SubscriptionSettings settings = SubscriptionSettings.builder()
                    .id(1L).price(BigDecimal.valueOf(500)).trialDays(7).announcementEnabled(false).build();
            when(subscriptionSettingsRepository.getSettings()).thenReturn(settings);
            when(userRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
            when(orderRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
            when(orderRepository.countCompletedBetween(any(), any())).thenReturn(0L);
            when(userSubscriptionRepository.countByStatusAndCreatedAtBetween(any(), any(), any())).thenReturn(0L);
            when(userSubscriptionRepository.count()).thenReturn(0L);
            when(userSubscriptionRepository.countCurrentlyActive(any())).thenReturn(0L);
            when(userSubscriptionRepository.countCurrentlyTrial(any())).thenReturn(0L);
            when(userSubscriptionRepository.countExpired(any())).thenReturn(0L);
            when(userSubscriptionRepository.countByStatus(any())).thenReturn(0L);
            when(userRepository.count()).thenReturn(10L);
            when(executorProfileRepository.count()).thenReturn(3L);
            when(userRepository.countVerifiedExecutors()).thenReturn(2L);
            when(orderRepository.count()).thenReturn(5L);
            when(orderRepository.countByStatus(any())).thenReturn(0L);
            when(orderResponseRepository.count()).thenReturn(0L);
            when(orderResponseRepository.countSelected()).thenReturn(0L);

            // Mock for getOverviewStats()
            when(userRepository.countByActiveTrue()).thenReturn(9L);
            when(userRepository.countByCreatedAtAfter(any())).thenReturn(1L);
            when(orderRepository.sumAgreedPrice()).thenReturn(BigDecimal.valueOf(10000));
            when(orderRepository.countWithAgreedPrice()).thenReturn(3L);
            when(reviewRepository.count()).thenReturn(2L);
            when(reviewRepository.countByIsModeratedFalse()).thenReturn(1L);
            when(reviewRepository.calculateOverallAverageRating()).thenReturn(4.5);
            when(orderRepository.countOrdersByCategory()).thenReturn(List.of());
            when(categoryRepository.count()).thenReturn(3L);

            byte[] csv = adminService.exportAnalyticsCsv();

            String csvString = new String(csv);
            assertThat(csvString).contains("Аналитика платформы FreelanceKG");
            assertThat(csvString).contains("ОБЗОР");
            assertThat(csvString).contains("ЕЖЕДНЕВНАЯ СТАТИСТИКА");
            assertThat(csvString).contains("ЕЖЕНЕДЕЛЬНАЯ СТАТИСТИКА");
            assertThat(csvString).contains("ЕЖЕМЕСЯЧНАЯ СТАТИСТИКА");
            assertThat(csvString).contains("ПОДПИСКИ");
            assertThat(csvString).contains("КОНВЕРСИИ");
        }
    }
}
