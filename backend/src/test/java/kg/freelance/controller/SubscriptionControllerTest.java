package kg.freelance.controller;

import kg.freelance.dto.response.AnnouncementResponse;
import kg.freelance.dto.response.MySubscriptionResponse;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.GlobalExceptionHandler;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionController Tests")
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionController subscriptionController;

    private MockMvc mockMvc;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(subscriptionController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        userPrincipal = UserPrincipal.builder()
                .id(1L).email("test@example.com").fullName("Test User")
                .role(UserRole.USER).active(true).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities()));
    }

    @Nested
    @DisplayName("GET /api/v1/subscription/announcement")
    class GetAnnouncementTests {

        @Test
        @DisplayName("Should return announcement")
        void shouldReturnAnnouncement() throws Exception {
            AnnouncementResponse response = AnnouncementResponse.builder()
                    .message("Welcome!").enabled(true).build();

            when(subscriptionService.getAnnouncement()).thenReturn(response);

            mockMvc.perform(get("/api/v1/subscription/announcement"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Welcome!"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/subscription/my")
    class GetMySubscriptionTests {

        @Test
        @DisplayName("Should return my subscription")
        void shouldReturnMySubscription() throws Exception {
            MySubscriptionResponse response = MySubscriptionResponse.builder()
                    .hasActiveSubscription(true).build();

            when(subscriptionService.getMySubscription(1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/subscription/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasActiveSubscription").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/subscription/start-trial")
    class StartTrialTests {

        @Test
        @DisplayName("Should start trial")
        void shouldStartTrial() throws Exception {
            MySubscriptionResponse response = MySubscriptionResponse.builder()
                    .hasActiveSubscription(true).build();

            when(subscriptionService.startTrial(1L)).thenReturn(response);

            mockMvc.perform(post("/api/v1/subscription/start-trial"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasActiveSubscription").value(true));
        }
    }
}
