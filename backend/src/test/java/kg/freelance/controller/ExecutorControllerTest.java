package kg.freelance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kg.freelance.dto.request.ExecutorProfileRequest;
import kg.freelance.dto.response.*;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.GlobalExceptionHandler;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.ExecutorService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutorController Tests")
class ExecutorControllerTest {

    @Mock
    private ExecutorService executorService;

    @InjectMocks
    private ExecutorController executorController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(executorController)
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

    private ExecutorResponse buildExecutorResponse() {
        return ExecutorResponse.builder()
                .id(1L).fullName("Executor User").bio("Bio")
                .specialization("Developer").rating(BigDecimal.valueOf(4.5))
                .completedOrders(10).availableForWork(true)
                .categories(List.of()).build();
    }

    @Nested
    @DisplayName("GET /api/v1/executors")
    class GetExecutorsTests {

        @Test
        @DisplayName("Should return executors list")
        void shouldReturnExecutorsList() throws Exception {
            ExecutorListResponse executor = ExecutorListResponse.builder()
                    .id(1L).fullName("Executor").rating(BigDecimal.valueOf(4.5))
                    .completedOrders(10).availableForWork(true).categories(List.of()).build();
            PageResponse<ExecutorListResponse> page = PageResponse.<ExecutorListResponse>builder()
                    .content(List.of(executor)).page(0).size(20)
                    .totalElements(1).totalPages(1).first(true).last(true).build();

            when(executorService.getExecutors(any(), any(), any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/executors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].fullName").value("Executor"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should apply filters")
        void shouldApplyFilters() throws Exception {
            PageResponse<ExecutorListResponse> page = PageResponse.<ExecutorListResponse>builder()
                    .content(List.of()).page(0).size(10)
                    .totalElements(0).totalPages(0).first(true).last(true).build();

            when(executorService.getExecutors(any(), any(), any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/executors")
                            .param("categoryId", "5")
                            .param("minRating", "4.0")
                            .param("availableOnly", "true")
                            .param("search", "java")
                            .param("sort", "orders"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/executors/{id}")
    class GetExecutorByIdTests {

        @Test
        @DisplayName("Should return executor by ID")
        void shouldReturnExecutorById() throws Exception {
            when(executorService.getExecutorById(1L)).thenReturn(buildExecutorResponse());

            mockMvc.perform(get("/api/v1/executors/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("Executor User"));
        }

        @Test
        @DisplayName("Should return 404 when not found")
        void shouldReturn404() throws Exception {
            when(executorService.getExecutorById(999L))
                    .thenThrow(new ResourceNotFoundException("Executor", "id", 999L));

            mockMvc.perform(get("/api/v1/executors/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/executors/me/profile")
    class CreateProfileTests {

        @Test
        @DisplayName("Should create executor profile")
        void shouldCreateProfile() throws Exception {
            ExecutorProfileRequest request = new ExecutorProfileRequest();
            request.setBio("My bio");
            request.setSpecialization("Developer");

            when(executorService.createOrUpdateProfile(eq(1L), any())).thenReturn(buildExecutorResponse());

            mockMvc.perform(post("/api/v1/executors/me/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.fullName").value("Executor User"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/executors/me/profile")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update executor profile")
        void shouldUpdateProfile() throws Exception {
            ExecutorProfileRequest request = new ExecutorProfileRequest();
            request.setBio("Updated bio");

            when(executorService.createOrUpdateProfile(eq(1L), any())).thenReturn(buildExecutorResponse());

            mockMvc.perform(put("/api/v1/executors/me/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/executors/me/categories")
    class UpdateCategoriesTests {

        @Test
        @DisplayName("Should update categories")
        void shouldUpdateCategories() throws Exception {
            when(executorService.updateCategories(eq(1L), anyList())).thenReturn(buildExecutorResponse());

            mockMvc.perform(put("/api/v1/executors/me/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[1, 2, 3]"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/executors/me/availability")
    class UpdateAvailabilityTests {

        @Test
        @DisplayName("Should update availability")
        void shouldUpdateAvailability() throws Exception {
            doNothing().when(executorService).updateAvailability(1L, false);

            mockMvc.perform(put("/api/v1/executors/me/availability")
                            .param("available", "false"))
                    .andExpect(status().isOk());

            verify(executorService).updateAvailability(1L, false);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/executors/{id}/reviews")
    class GetExecutorReviewsTests {

        @Test
        @DisplayName("Should return executor reviews")
        void shouldReturnReviews() throws Exception {
            PageResponse<ReviewResponse> page = PageResponse.<ReviewResponse>builder()
                    .content(List.of()).page(0).size(10)
                    .totalElements(0).totalPages(0).first(true).last(true).build();

            when(executorService.getExecutorReviews(eq(1L), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/executors/1/reviews"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/executors/{id}/portfolio")
    class GetExecutorPortfolioTests {

        @Test
        @DisplayName("Should return executor portfolio")
        void shouldReturnPortfolio() throws Exception {
            PortfolioResponse item = PortfolioResponse.builder()
                    .id(1L).title("Project").build();

            when(executorService.getExecutorPortfolio(1L)).thenReturn(List.of(item));

            mockMvc.perform(get("/api/v1/executors/1/portfolio"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Project"));
        }
    }
}
