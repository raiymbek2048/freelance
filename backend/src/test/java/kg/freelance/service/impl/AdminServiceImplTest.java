package kg.freelance.service.impl;

import kg.freelance.dto.request.CategoryCreateRequest;
import kg.freelance.dto.response.AdminOrderResponse;
import kg.freelance.dto.response.AdminUserResponse;
import kg.freelance.dto.response.CategoryResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.entity.*;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.entity.enums.ProfileVisibility;
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
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            // When
            adminService.resolveDispute(1L, true, "Client was right");

            // Then
            verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CANCELLED));
        }

        @Test
        @DisplayName("Should resolve dispute in favor of executor")
        void shouldResolveDisputeInFavorOfExecutor() {
            // Given
            order.setStatus(OrderStatus.DISPUTED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(executorProfileRepository.findById(2L)).thenReturn(Optional.of(executorProfile));

            // When
            adminService.resolveDispute(1L, false, "Executor was right");

            // Then
            verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.COMPLETED));
            verify(executorProfileRepository).save(any(ExecutorProfile.class));
        }

        @Test
        @DisplayName("Should throw exception when order is not in dispute")
        void shouldThrowExceptionWhenOrderNotInDispute() {
            // Given
            order.setStatus(OrderStatus.IN_PROGRESS);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            // When/Then
            assertThatThrownBy(() -> adminService.resolveDispute(1L, true, "Resolution"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Order is not in dispute");
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
}
