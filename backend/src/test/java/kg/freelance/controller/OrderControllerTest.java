package kg.freelance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kg.freelance.dto.request.OrderCreateRequest;
import kg.freelance.dto.request.OrderResponseRequest;
import kg.freelance.dto.request.SelectExecutorRequest;
import kg.freelance.dto.response.*;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ForbiddenException;
import kg.freelance.exception.GlobalExceptionHandler;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.OrderService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        userPrincipal = UserPrincipal.builder()
                .id(1L).email("test@example.com").fullName("Test User")
                .role(UserRole.USER).active(true).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities()));
    }

    private OrderDetailResponse buildOrderDetail() {
        return OrderDetailResponse.builder()
                .id(1L).title("Test Order").description("Description")
                .status(OrderStatus.NEW)
                .budgetMin(BigDecimal.valueOf(1000))
                .budgetMax(BigDecimal.valueOf(5000))
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/orders")
    class GetPublicOrdersTests {

        @Test
        @DisplayName("Should return paginated public orders")
        void shouldReturnPaginatedPublicOrders() throws Exception {
            OrderListResponse order = OrderListResponse.builder()
                    .id(1L).title("Test Order").status(OrderStatus.NEW).build();
            PageResponse<OrderListResponse> page = PageResponse.<OrderListResponse>builder()
                    .content(List.of(order)).page(0).size(20)
                    .totalElements(1).totalPages(1).first(true).last(true).build();

            when(orderService.getPublicOrders(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Test Order"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should pass filter params correctly")
        void shouldPassFilterParamsCorrectly() throws Exception {
            PageResponse<OrderListResponse> page = PageResponse.<OrderListResponse>builder()
                    .content(List.of()).page(0).size(20)
                    .totalElements(0).totalPages(0).first(true).last(true).build();

            when(orderService.getPublicOrders(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/orders")
                            .param("categoryId", "5")
                            .param("budgetMin", "1000")
                            .param("budgetMax", "5000")
                            .param("search", "web")
                            .param("sort", "budget")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());

            verify(orderService).getPublicOrders(
                    eq(5L), eq(BigDecimal.valueOf(1000)), eq(BigDecimal.valueOf(5000)),
                    eq("web"), isNull(), eq(1L), any());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders/{id}")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should return order by ID")
        void shouldReturnOrderById() throws Exception {
            when(orderService.getOrderById(1L, 1L)).thenReturn(buildOrderDetail());

            mockMvc.perform(get("/api/v1/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Test Order"));
        }

        @Test
        @DisplayName("Should return 404 when order not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(orderService.getOrderById(999L, 1L))
                    .thenThrow(new ResourceNotFoundException("Order", "id", 999L));

            mockMvc.perform(get("/api/v1/orders/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/orders")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully")
        void shouldCreateOrderSuccessfully() throws Exception {
            OrderCreateRequest request = new OrderCreateRequest();
            request.setTitle("New Order");
            request.setDescription("Order description");
            request.setCategoryId(1L);

            when(orderService.createOrder(eq(1L), any(OrderCreateRequest.class)))
                    .thenReturn(buildOrderDetail());

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/orders/{id}")
    class UpdateOrderTests {

        @Test
        @DisplayName("Should update order successfully")
        void shouldUpdateOrderSuccessfully() throws Exception {
            when(orderService.updateOrder(eq(1L), eq(1L), any())).thenReturn(buildOrderDetail());

            mockMvc.perform(put("/api/v1/orders/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Updated\",\"description\":\"Updated desc\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/orders/{id}")
    class DeleteOrderTests {

        @Test
        @DisplayName("Should delete order successfully")
        void shouldDeleteOrderSuccessfully() throws Exception {
            doNothing().when(orderService).deleteOrder(1L, 1L);

            mockMvc.perform(delete("/api/v1/orders/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 403 when not the owner")
        void shouldReturn403WhenNotOwner() throws Exception {
            doThrow(new ForbiddenException("Only the client can delete the order"))
                    .when(orderService).deleteOrder(1L, 1L);

            mockMvc.perform(delete("/api/v1/orders/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Order State Transitions")
    class OrderStateTransitionTests {

        @Test
        @DisplayName("POST /api/v1/orders/{id}/select-executor")
        void shouldSelectExecutor() throws Exception {
            SelectExecutorRequest request = new SelectExecutorRequest();
            request.setResponseId(2L);

            doNothing().when(orderService).selectExecutor(eq(1L), eq(1L), any());

            mockMvc.perform(post("/api/v1/orders/1/select-executor")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/v1/orders/{id}/submit-for-review")
        void shouldSubmitForReview() throws Exception {
            doNothing().when(orderService).submitForReview(1L, 1L);

            mockMvc.perform(post("/api/v1/orders/1/submit-for-review"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/v1/orders/{id}/approve")
        void shouldApproveWork() throws Exception {
            doNothing().when(orderService).approveWork(1L, 1L);

            mockMvc.perform(post("/api/v1/orders/1/approve"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/v1/orders/{id}/request-revision")
        void shouldRequestRevision() throws Exception {
            doNothing().when(orderService).requestRevision(1L, 1L, "Need changes");

            mockMvc.perform(post("/api/v1/orders/1/request-revision")
                            .param("reason", "Need changes"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/v1/orders/{id}/dispute")
        void shouldOpenDispute() throws Exception {
            doNothing().when(orderService).openDispute(1L, 1L, "Issue");

            mockMvc.perform(post("/api/v1/orders/1/dispute")
                            .param("reason", "Issue"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 400 for invalid state transition")
        void shouldReturn400ForInvalidTransition() throws Exception {
            doThrow(new BadRequestException("Invalid status transition"))
                    .when(orderService).submitForReview(1L, 1L);

            mockMvc.perform(post("/api/v1/orders/1/submit-for-review"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("My Orders")
    class MyOrdersTests {

        @Test
        @DisplayName("GET /api/v1/orders/my/as-client")
        void shouldGetMyOrdersAsClient() throws Exception {
            PageResponse<OrderListResponse> page = PageResponse.<OrderListResponse>builder()
                    .content(List.of()).page(0).size(20)
                    .totalElements(0).totalPages(0).first(true).last(true).build();

            when(orderService.getMyOrdersAsClient(eq(1L), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/orders/my/as-client"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/v1/orders/my/as-executor")
        void shouldGetMyOrdersAsExecutor() throws Exception {
            PageResponse<OrderListResponse> page = PageResponse.<OrderListResponse>builder()
                    .content(List.of()).page(0).size(20)
                    .totalElements(0).totalPages(0).first(true).last(true).build();

            when(orderService.getMyOrdersAsExecutor(eq(1L), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/orders/my/as-executor"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Order Responses")
    class OrderResponsesTests {

        @Test
        @DisplayName("POST /api/v1/orders/{id}/responses - create response")
        void shouldCreateResponse() throws Exception {
            OrderResponseRequest request = new OrderResponseRequest();
            request.setCoverLetter("I can do this");
            request.setProposedPrice(BigDecimal.valueOf(3000));

            OrderResponseDto response = OrderResponseDto.builder()
                    .id(1L).coverLetter("I can do this")
                    .proposedPrice(BigDecimal.valueOf(3000)).build();

            when(orderService.createResponse(eq(1L), eq(1L), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/orders/1/responses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.coverLetter").value("I can do this"));
        }

        @Test
        @DisplayName("GET /api/v1/orders/{id}/responses - get responses")
        void shouldGetOrderResponses() throws Exception {
            when(orderService.getOrderResponses(1L, 1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/orders/1/responses"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE /api/v1/orders/responses/{id} - delete response")
        void shouldDeleteResponse() throws Exception {
            doNothing().when(orderService).deleteResponse(1L, 1L);

            mockMvc.perform(delete("/api/v1/orders/responses/1"))
                    .andExpect(status().isNoContent());
        }
    }
}
