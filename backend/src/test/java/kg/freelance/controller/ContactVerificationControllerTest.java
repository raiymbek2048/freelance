package kg.freelance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kg.freelance.dto.request.SendCodeRequest;
import kg.freelance.dto.request.VerifyCodeRequest;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.entity.enums.VerificationType;
import kg.freelance.exception.GlobalExceptionHandler;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.VerificationCodeService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContactVerificationController Tests")
class ContactVerificationControllerTest {

    @Mock
    private VerificationCodeService verificationCodeService;

    @InjectMocks
    private ContactVerificationController controller;

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
    @DisplayName("POST /api/v1/contact-verification/send-code")
    class SendCodeTests {

        @Test
        @DisplayName("Should send email verification code")
        void shouldSendEmailCode() throws Exception {
            SendCodeRequest request = new SendCodeRequest();
            request.setType(VerificationType.EMAIL);

            doNothing().when(verificationCodeService).sendVerificationCode(1L, VerificationType.EMAIL);

            mockMvc.perform(post("/api/v1/contact-verification/send-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());

            verify(verificationCodeService).sendVerificationCode(1L, VerificationType.EMAIL);
        }

        @Test
        @DisplayName("Should send phone verification code")
        void shouldSendPhoneCode() throws Exception {
            SendCodeRequest request = new SendCodeRequest();
            request.setType(VerificationType.PHONE);

            doNothing().when(verificationCodeService).sendVerificationCode(1L, VerificationType.PHONE);

            mockMvc.perform(post("/api/v1/contact-verification/send-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/contact-verification/verify")
    class VerifyCodeTests {

        @Test
        @DisplayName("Should verify code successfully")
        void shouldVerifyCodeSuccessfully() throws Exception {
            VerifyCodeRequest request = new VerifyCodeRequest();
            request.setType(VerificationType.EMAIL);
            request.setCode("123456");

            when(verificationCodeService.verifyCode(1L, "123456", VerificationType.EMAIL)).thenReturn(true);

            mockMvc.perform(post("/api/v1/contact-verification/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should return 400 for invalid code")
        void shouldReturn400ForInvalidCode() throws Exception {
            VerifyCodeRequest request = new VerifyCodeRequest();
            request.setType(VerificationType.EMAIL);
            request.setCode("000000");

            when(verificationCodeService.verifyCode(1L, "000000", VerificationType.EMAIL)).thenReturn(false);

            mockMvc.perform(post("/api/v1/contact-verification/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/contact-verification/can-request")
    class CanRequestCodeTests {

        @Test
        @DisplayName("Should return true when can request")
        void shouldReturnTrueWhenCanRequest() throws Exception {
            when(verificationCodeService.canRequestNewCode(1L, VerificationType.EMAIL)).thenReturn(true);

            mockMvc.perform(get("/api/v1/contact-verification/can-request")
                            .param("type", "EMAIL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.canRequest").value(true));
        }

        @Test
        @DisplayName("Should return false when rate limited")
        void shouldReturnFalseWhenRateLimited() throws Exception {
            when(verificationCodeService.canRequestNewCode(1L, VerificationType.PHONE)).thenReturn(false);

            mockMvc.perform(get("/api/v1/contact-verification/can-request")
                            .param("type", "PHONE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.canRequest").value(false));
        }
    }
}
