package kg.freelance.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kg.freelance.dto.request.GrantSubscriptionRequest;
import kg.freelance.dto.request.SubscriptionSettingsRequest;
import kg.freelance.dto.response.*;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.entity.enums.SubscriptionStatus;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.GlobalExceptionHandler;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.AdminService;
import kg.freelance.service.CategoryService;
import kg.freelance.service.ExecutorVerificationService;
import kg.freelance.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Controller Tests")
class AdminControllerTests {

    @Mock
    private AdminService adminService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ExecutorVerificationService verificationService;

    @Mock
    private SubscriptionService subscriptionService;

    private ObjectMapper objectMapper;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        adminPrincipal = UserPrincipal.builder()
                .id(1L).email("admin@example.com").fullName("Admin")
                .role(UserRole.ADMIN).active(true).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities()));
    }

    @Nested
    @DisplayName("AdminUserController Tests")
    class AdminUserControllerTests {

        private MockMvc mockMvc;

        @BeforeEach
        void setUpMvc() {
            AdminUserController controller = new AdminUserController(adminService);
            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();
        }

        @Test
        @DisplayName("GET /api/v1/admin/users - should return users")
        void shouldReturnUsers() throws Exception {
            PageResponse<AdminUserResponse> page = PageResponse.<AdminUserResponse>builder()
                    .content(List.of()).page(0).size(20)
                    .totalElements(0).totalPages(0).first(true).last(true).build();

            when(adminService.getAllUsers(any(), any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/v1/admin/users/{id} - should return user by ID")
        void shouldReturnUserById() throws Exception {
            AdminUserResponse user = AdminUserResponse.builder()
                    .id(5L).email("user@example.com").fullName("User").build();

            when(adminService.getUserById(5L)).thenReturn(user);

            mockMvc.perform(get("/api/v1/admin/users/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5));
        }

        @Test
        @DisplayName("PUT /api/v1/admin/users/{id}/block - should block user")
        void shouldBlockUser() throws Exception {
            doNothing().when(adminService).blockUser(5L);

            mockMvc.perform(put("/api/v1/admin/users/5/block"))
                    .andExpect(status().isOk());

            verify(adminService).blockUser(5L);
        }

        @Test
        @DisplayName("PUT /api/v1/admin/users/{id}/unblock - should unblock user")
        void shouldUnblockUser() throws Exception {
            doNothing().when(adminService).unblockUser(5L);

            mockMvc.perform(put("/api/v1/admin/users/5/unblock"))
                    .andExpect(status().isOk());

            verify(adminService).unblockUser(5L);
        }

        @Test
        @DisplayName("PUT /api/v1/admin/users/{id}/role - should change role")
        void shouldChangeRole() throws Exception {
            doNothing().when(adminService).changeUserRole(5L, UserRole.ADMIN);

            mockMvc.perform(put("/api/v1/admin/users/5/role")
                            .param("role", "ADMIN"))
                    .andExpect(status().isOk());

            verify(adminService).changeUserRole(5L, UserRole.ADMIN);
        }
    }

    @Nested
    @DisplayName("AdminOrderController Tests")
    class AdminOrderControllerTests {

        private MockMvc mockMvc;

        @BeforeEach
        void setUpMvc() {
            AdminOrderController controller = new AdminOrderController(adminService);
            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();
        }

        @Test
        @DisplayName("GET /api/v1/admin/orders - should return orders")
        void shouldReturnOrders() throws Exception {
            PageResponse<AdminOrderResponse> page = PageResponse.<AdminOrderResponse>builder()
                    .content(List.of()).page(0).size(20)
                    .totalElements(0).totalPages(0).first(true).last(true).build();

            when(adminService.getAllOrders(any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/orders"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/v1/admin/orders/disputes - should return disputed orders")
        void shouldReturnDisputedOrders() throws Exception {
            PageResponse<AdminOrderResponse> page = PageResponse.<AdminOrderResponse>builder()
                    .content(List.of()).page(0).size(20)
                    .totalElements(0).totalPages(0).first(true).last(true).build();

            when(adminService.getDisputedOrders(any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/orders/disputes"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT /api/v1/admin/orders/{id}/resolve-dispute")
        void shouldResolveDispute() throws Exception {
            doNothing().when(adminService).resolveDispute(1L, true, "Client wins");

            mockMvc.perform(put("/api/v1/admin/orders/1/resolve-dispute")
                            .param("favorClient", "true")
                            .param("resolution", "Client wins"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE /api/v1/admin/orders/{id} - should delete order")
        void shouldDeleteOrder() throws Exception {
            doNothing().when(adminService).deleteOrder(1L);

            mockMvc.perform(delete("/api/v1/admin/orders/1"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("AdminCategoryController Tests")
    class AdminCategoryControllerTests {

        private MockMvc mockMvc;

        @BeforeEach
        void setUpMvc() {
            AdminCategoryController controller = new AdminCategoryController(adminService, categoryService);
            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();
        }

        @Test
        @DisplayName("GET /api/v1/admin/categories - should return categories")
        void shouldReturnCategories() throws Exception {
            when(categoryService.getAllCategories()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/admin/categories"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/v1/admin/categories - should create category")
        void shouldCreateCategory() throws Exception {
            CategoryResponse response = CategoryResponse.builder()
                    .id(1L).name("New Cat").slug("new-cat").build();

            when(adminService.createCategory(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New Cat\",\"slug\":\"new-cat\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("New Cat"));
        }

        @Test
        @DisplayName("DELETE /api/v1/admin/categories/{id} - should delete category")
        void shouldDeleteCategory() throws Exception {
            doNothing().when(adminService).deleteCategory(1L);

            mockMvc.perform(delete("/api/v1/admin/categories/1"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("AdminStatsController Tests")
    class AdminStatsControllerTests {

        private MockMvc mockMvc;

        @BeforeEach
        void setUpMvc() {
            AdminStatsController controller = new AdminStatsController(adminService);
            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();
        }

        @Test
        @DisplayName("GET /api/v1/admin/stats/overview - should return stats")
        void shouldReturnOverviewStats() throws Exception {
            AdminStatsResponse stats = AdminStatsResponse.builder()
                    .totalUsers(100L).totalOrders(50L).build();

            when(adminService.getOverviewStats()).thenReturn(stats);

            mockMvc.perform(get("/api/v1/admin/stats/overview"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalUsers").value(100));
        }

        @Test
        @DisplayName("GET /api/v1/admin/stats/analytics - should return analytics")
        void shouldReturnAnalytics() throws Exception {
            AnalyticsResponse analytics = AnalyticsResponse.builder().build();

            when(adminService.getAnalytics()).thenReturn(analytics);

            mockMvc.perform(get("/api/v1/admin/stats/analytics"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("AdminReviewController Tests")
    class AdminReviewControllerTests {

        private MockMvc mockMvc;

        @BeforeEach
        void setUpMvc() {
            AdminReviewController controller = new AdminReviewController(adminService);
            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();
        }

        @Test
        @DisplayName("GET /api/v1/admin/reviews - should return reviews")
        void shouldReturnReviews() throws Exception {
            PageResponse<AdminReviewResponse> page = PageResponse.<AdminReviewResponse>builder()
                    .content(List.of()).page(0).size(20)
                    .totalElements(0).totalPages(0).first(true).last(true).build();

            when(adminService.getAllReviews(any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/reviews"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT /api/v1/admin/reviews/{id}/approve - should approve review")
        void shouldApproveReview() throws Exception {
            doNothing().when(adminService).approveReview(1L);

            mockMvc.perform(put("/api/v1/admin/reviews/1/approve"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT /api/v1/admin/reviews/{id}/reject - should reject review")
        void shouldRejectReview() throws Exception {
            doNothing().when(adminService).rejectReview(1L, "Inappropriate");

            mockMvc.perform(put("/api/v1/admin/reviews/1/reject")
                            .param("reason", "Inappropriate"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE /api/v1/admin/reviews/{id} - should delete review")
        void shouldDeleteReview() throws Exception {
            doNothing().when(adminService).deleteReview(1L);

            mockMvc.perform(delete("/api/v1/admin/reviews/1"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("AdminVerificationController Tests")
    class AdminVerificationControllerTests {

        private MockMvc mockMvc;

        @BeforeEach
        void setUpMvc() {
            AdminVerificationController controller = new AdminVerificationController(verificationService);
            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();
        }

        @Test
        @DisplayName("GET /api/v1/admin/verifications - should return verifications")
        void shouldReturnVerifications() throws Exception {
            PageResponse<AdminVerificationResponse> page = PageResponse.<AdminVerificationResponse>builder()
                    .content(List.of()).page(0).size(20)
                    .totalElements(0).totalPages(0).first(true).last(true).build();

            when(verificationService.getAllVerifications(any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/verifications"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/v1/admin/verifications/count - should return pending count")
        void shouldReturnPendingCount() throws Exception {
            when(verificationService.countPending()).thenReturn(5L);

            mockMvc.perform(get("/api/v1/admin/verifications/count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pending").value(5));
        }

        @Test
        @DisplayName("PUT /api/v1/admin/verifications/{userId}/approve")
        void shouldApproveVerification() throws Exception {
            doNothing().when(verificationService).approveVerification(5L, 1L);

            mockMvc.perform(put("/api/v1/admin/verifications/5/approve"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT /api/v1/admin/verifications/{userId}/reject")
        void shouldRejectVerification() throws Exception {
            doNothing().when(verificationService).rejectVerification(5L, 1L, "Bad docs");

            mockMvc.perform(put("/api/v1/admin/verifications/5/reject")
                            .param("reason", "Bad docs"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("AdminSubscriptionController Tests")
    class AdminSubscriptionControllerTests {

        private MockMvc mockMvc;

        @BeforeEach
        void setUpMvc() {
            AdminSubscriptionController controller = new AdminSubscriptionController(subscriptionService);
            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();
            objectMapper.registerModule(new JavaTimeModule());
        }

        @Test
        @DisplayName("GET /api/v1/admin/subscription/settings - should return settings")
        void shouldReturnSettings() throws Exception {
            SubscriptionSettingsResponse settings = SubscriptionSettingsResponse.builder()
                    .trialDays(7).announcementEnabled(true).build();

            when(subscriptionService.getSettings()).thenReturn(settings);

            mockMvc.perform(get("/api/v1/admin/subscription/settings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trialDays").value(7));
        }

        @Test
        @DisplayName("PUT /api/v1/admin/subscription/settings - should update settings")
        void shouldUpdateSettings() throws Exception {
            SubscriptionSettingsRequest request = new SubscriptionSettingsRequest();
            request.setTrialDays(14);
            request.setAnnouncementEnabled(false);

            SubscriptionSettingsResponse response = SubscriptionSettingsResponse.builder()
                    .trialDays(14).announcementEnabled(false).build();

            when(subscriptionService.updateSettings(any(), eq(1L))).thenReturn(response);

            mockMvc.perform(put("/api/v1/admin/subscription/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trialDays").value(14));
        }

        @Test
        @DisplayName("GET /api/v1/admin/subscription/users - should return subscriptions")
        void shouldReturnSubscriptions() throws Exception {
            when(subscriptionService.getAllSubscriptions(any()))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/v1/admin/subscription/users"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/v1/admin/subscription/users/{userId}/grant - should grant subscription")
        void shouldGrantSubscription() throws Exception {
            GrantSubscriptionRequest request = new GrantSubscriptionRequest();
            request.setDays(30);

            UserSubscriptionResponse response = UserSubscriptionResponse.builder()
                    .id(1L).userId(5L).status(SubscriptionStatus.ACTIVE).isActive(true).build();

            when(subscriptionService.grantSubscription(eq(5L), any(), eq(1L))).thenReturn(response);

            mockMvc.perform(post("/api/v1/admin/subscription/users/5/grant")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(5));
        }

        @Test
        @DisplayName("DELETE /api/v1/admin/subscription/users/{userId} - should revoke subscription")
        void shouldRevokeSubscription() throws Exception {
            doNothing().when(subscriptionService).revokeSubscription(5L);

            mockMvc.perform(delete("/api/v1/admin/subscription/users/5"))
                    .andExpect(status().isOk());

            verify(subscriptionService).revokeSubscription(5L);
        }
    }
}
