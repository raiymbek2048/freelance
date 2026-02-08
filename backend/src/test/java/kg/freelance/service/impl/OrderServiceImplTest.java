package kg.freelance.service.impl;

import kg.freelance.dto.request.OrderCreateRequest;
import kg.freelance.dto.request.OrderResponseRequest;
import kg.freelance.dto.request.SelectExecutorRequest;
import kg.freelance.dto.response.OrderDetailResponse;
import kg.freelance.dto.response.OrderListResponse;
import kg.freelance.dto.response.OrderResponseDto;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.entity.*;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.entity.enums.ProfileVisibility;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ForbiddenException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.MessageRepository;
import kg.freelance.repository.*;
import kg.freelance.service.ChatService;
import kg.freelance.service.EmailService;
import kg.freelance.service.ExecutorVerificationService;
import kg.freelance.service.SubscriptionService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private OrderResponseRepository orderResponseRepository;

    @Mock
    private ExecutorProfileRepository executorProfileRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ExecutorVerificationService executorVerificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private ChatService chatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testClient;
    private User testExecutor;
    private Category testCategory;
    private Order testOrder;
    private ExecutorProfile executorProfile;

    @BeforeEach
    void setUp() {
        testClient = User.builder()
                .id(1L)
                .email("client@example.com")
                .fullName("Test Client")
                .role(UserRole.USER)
                .profileVisibility(ProfileVisibility.PUBLIC)
                .active(true)
                .executorVerified(false)
                .build();

        executorProfile = new ExecutorProfile();
        executorProfile.setId(2L);
        executorProfile.setTotalOrders(5);
        executorProfile.setCompletedOrders(3);
        executorProfile.setRating(BigDecimal.valueOf(4.5));
        executorProfile.setReviewCount(3);

        testExecutor = User.builder()
                .id(2L)
                .email("executor@example.com")
                .fullName("Test Executor")
                .role(UserRole.USER)
                .profileVisibility(ProfileVisibility.PUBLIC)
                .active(true)
                .executorVerified(true)
                .executorProfile(executorProfile)
                .build();
        executorProfile.setUser(testExecutor);

        testCategory = Category.builder()
                .id(1L)
                .name("Web Development")
                .slug("web-development")
                .active(true)
                .build();

        testOrder = Order.builder()
                .id(1L)
                .title("Test Order")
                .description("Test description for the order")
                .client(testClient)
                .category(testCategory)
                .status(OrderStatus.NEW)
                .budgetMin(BigDecimal.valueOf(1000))
                .budgetMax(BigDecimal.valueOf(5000))
                .deadline(LocalDate.now().plusDays(7))
                .location("Бишкек")
                .isPublic(true)
                .viewCount(0)
                .responseCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully")
        void shouldCreateOrderSuccessfully() {
            // Given
            OrderCreateRequest request = new OrderCreateRequest();
            request.setTitle("New Order");
            request.setDescription("Order description");
            request.setCategoryId(1L);
            request.setBudgetMin(BigDecimal.valueOf(1000));
            request.setBudgetMax(BigDecimal.valueOf(5000));
            request.setDeadline(LocalDate.now().plusDays(14));
            request.setLocation("Бишкек");
            request.setIsPublic(true);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testClient));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                order.setCreatedAt(LocalDateTime.now());
                return order;
            });

            // When
            OrderDetailResponse response = orderService.createOrder(1L, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("New Order");
            assertThat(response.getStatus()).isEqualTo(OrderStatus.NEW);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void shouldThrowExceptionWhenCategoryNotFound() {
            // Given
            OrderCreateRequest request = new OrderCreateRequest();
            request.setTitle("New Order");
            request.setCategoryId(999L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testClient));
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Orders Tests")
    class GetOrdersTests {

        @Test
        @DisplayName("Should return paginated orders")
        void shouldReturnPaginatedOrders() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder), pageable, 1);

            when(orderRepository.findPublicOrders(
                    isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
            )).thenReturn(orderPage);

            // When
            PageResponse<OrderListResponse> response = orderService.getPublicOrders(
                    null, null, null, null, null, null, pageable
            );

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should filter orders by category")
        void shouldFilterOrdersByCategory() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder), pageable, 1);

            when(orderRepository.findPublicOrders(
                    eq(1L), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
            )).thenReturn(orderPage);

            // When
            PageResponse<OrderListResponse> response = orderService.getPublicOrders(
                    1L, null, null, null, null, null, pageable
            );

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            verify(orderRepository).findPublicOrders(eq(1L), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Get Order Detail Tests")
    class GetOrderDetailTests {

        @Test
        @DisplayName("Should return order detail for owner")
        void shouldReturnOrderDetailForOwner() {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // When
            OrderDetailResponse response = orderService.getOrderById(1L, 1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getTitle()).isEqualTo("Test Order");
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Given
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> orderService.getOrderById(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Create Response Tests")
    class CreateResponseTests {

        @Test
        @DisplayName("Should create response successfully")
        void shouldCreateResponseSuccessfully() {
            // Given
            OrderResponseRequest request = new OrderResponseRequest();
            request.setCoverLetter("I can do this job");
            request.setProposedPrice(BigDecimal.valueOf(3000));
            request.setProposedDays(5);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderResponseRepository.existsByOrderIdAndExecutorId(1L, 2L)).thenReturn(false);
            when(executorProfileRepository.existsById(2L)).thenReturn(true);
            when(userRepository.findById(2L)).thenReturn(Optional.of(testExecutor));
            when(subscriptionService.canAccessOrders(testExecutor)).thenReturn(true);
            when(orderResponseRepository.save(any(OrderResponse.class))).thenAnswer(invocation -> {
                OrderResponse resp = invocation.getArgument(0);
                resp.setId(1L);
                resp.setCreatedAt(LocalDateTime.now());
                return resp;
            });
            when(executorProfileRepository.findById(2L)).thenReturn(Optional.of(executorProfile));

            // When
            OrderResponseDto response = orderService.createResponse(2L, 1L, request);

            // Then
            assertThat(response).isNotNull();
            verify(orderResponseRepository).save(any(OrderResponse.class));
        }

        @Test
        @DisplayName("Should throw exception when executor not verified")
        void shouldThrowExceptionWhenExecutorNotVerified() {
            // Given
            testExecutor.setExecutorVerified(false);
            OrderResponseRequest request = new OrderResponseRequest();
            request.setCoverLetter("I can do this");

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderResponseRepository.existsByOrderIdAndExecutorId(1L, 2L)).thenReturn(false);
            when(executorProfileRepository.existsById(2L)).thenReturn(true);
            when(userRepository.findById(2L)).thenReturn(Optional.of(testExecutor));
            when(subscriptionService.canAccessOrders(testExecutor)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> orderService.createResponse(2L, 1L, request))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Should throw exception when already responded")
        void shouldThrowExceptionWhenAlreadyResponded() {
            // Given
            OrderResponseRequest request = new OrderResponseRequest();
            request.setCoverLetter("I can do this");

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderResponseRepository.existsByOrderIdAndExecutorId(1L, 2L)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> orderService.createResponse(2L, 1L, request))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw exception when responding to own order")
        void shouldThrowExceptionWhenRespondingToOwnOrder() {
            // Given
            OrderResponseRequest request = new OrderResponseRequest();
            request.setCoverLetter("I can do this");

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // When/Then - client's ID is 1L, and testOrder's client is testClient with ID 1L
            assertThatThrownBy(() -> orderService.createResponse(1L, 1L, request))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("Select Executor Tests")
    class SelectExecutorTests {

        @Test
        @DisplayName("Should select executor successfully")
        void shouldSelectExecutorSuccessfully() {
            // Given
            OrderResponse orderResponse = OrderResponse.builder()
                    .id(1L)
                    .order(testOrder)
                    .executor(testExecutor)
                    .coverLetter("I can do this")
                    .proposedPrice(BigDecimal.valueOf(3000))
                    .proposedDays(5)
                    .isSelected(false)
                    .build();

            SelectExecutorRequest request = new SelectExecutorRequest();
            request.setResponseId(1L);
            request.setAgreedPrice(BigDecimal.valueOf(3000));
            request.setAgreedDeadline(LocalDate.now().plusDays(5));

            ChatRoom chatRoom = new ChatRoom();
            chatRoom.setId(1L);

            Message systemMessage = Message.builder()
                    .id(1L)
                    .chatRoom(chatRoom)
                    .content("System message")
                    .build();

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderResponseRepository.findById(1L)).thenReturn(Optional.of(orderResponse));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);
            when(messageRepository.save(any(Message.class))).thenReturn(systemMessage);
            when(executorProfileRepository.findById(2L)).thenReturn(Optional.of(executorProfile));

            // When
            orderService.selectExecutor(1L, 1L, request);

            // Then
            verify(orderRepository).save(any(Order.class));
            verify(orderResponseRepository).save(any(OrderResponse.class));
            verify(executorProfileRepository).save(any(ExecutorProfile.class));
        }

        @Test
        @DisplayName("Should throw exception when not order owner")
        void shouldThrowExceptionWhenNotOrderOwner() {
            // Given
            SelectExecutorRequest request = new SelectExecutorRequest();
            request.setResponseId(1L);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // When/Then
            assertThatThrownBy(() -> orderService.selectExecutor(999L, 1L, request))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Should throw exception when order not in NEW status")
        void shouldThrowExceptionWhenOrderNotNew() {
            // Given
            testOrder.setStatus(OrderStatus.IN_PROGRESS);
            SelectExecutorRequest request = new SelectExecutorRequest();
            request.setResponseId(1L);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // When/Then
            assertThatThrownBy(() -> orderService.selectExecutor(1L, 1L, request))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("Delete Order Tests")
    class DeleteOrderTests {

        @Test
        @DisplayName("Should delete order by owner")
        void shouldDeleteOrderByOwner() {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // When
            orderService.deleteOrder(1L, 1L);

            // Then
            verify(orderRepository).delete(testOrder);
        }

        @Test
        @DisplayName("Should throw exception when deleting in progress order")
        void shouldThrowExceptionWhenDeletingInProgressOrder() {
            // Given
            testOrder.setStatus(OrderStatus.IN_PROGRESS);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // When/Then
            assertThatThrownBy(() -> orderService.deleteOrder(1L, 1L))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
