package kg.freelance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kg.freelance.dto.request.VerificationSubmitRequest;
import kg.freelance.dto.response.VerificationResponse;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.entity.enums.VerificationStatus;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.GlobalExceptionHandler;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.ExecutorVerificationService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutorVerificationController Tests")
class ExecutorVerificationControllerTest {

    @Mock
    private ExecutorVerificationService verificationService;

    @InjectMocks
    private ExecutorVerificationController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
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

    @Nested
    @DisplayName("GET /api/v1/verification/status")
    class GetMyStatusTests {

        @Test
        @DisplayName("Should return verification status")
        void shouldReturnVerificationStatus() throws Exception {
            VerificationResponse response = VerificationResponse.builder()
                    .status(VerificationStatus.PENDING)
                    .submittedAt(LocalDateTime.now())
                    .build();

            when(verificationService.getMyStatus(1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/verification/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("Should return NONE status when not submitted")
        void shouldReturnNoneWhenNotSubmitted() throws Exception {
            VerificationResponse response = VerificationResponse.builder()
                    .status(VerificationStatus.NONE).build();

            when(verificationService.getMyStatus(1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/verification/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("NONE"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/verification/submit")
    class SubmitVerificationTests {

        @Test
        @DisplayName("Should submit verification successfully")
        void shouldSubmitVerification() throws Exception {
            VerificationSubmitRequest request = new VerificationSubmitRequest();
            request.setPassportUrl("https://s3.example.com/passport.jpg");
            request.setSelfieUrl("https://s3.example.com/selfie.jpg");

            VerificationResponse response = VerificationResponse.builder()
                    .status(VerificationStatus.PENDING)
                    .submittedAt(LocalDateTime.now())
                    .build();

            when(verificationService.submitVerification(eq(1L), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/verification/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("Should return 400 when already pending")
        void shouldReturn400WhenAlreadyPending() throws Exception {
            VerificationSubmitRequest request = new VerificationSubmitRequest();
            request.setPassportUrl("https://s3.example.com/passport.jpg");
            request.setSelfieUrl("https://s3.example.com/selfie.jpg");

            when(verificationService.submitVerification(eq(1L), any()))
                    .thenThrow(new BadRequestException("Verification already pending"));

            mockMvc.perform(post("/api/v1/verification/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
