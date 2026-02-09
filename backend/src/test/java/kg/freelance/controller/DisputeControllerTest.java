package kg.freelance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kg.freelance.dto.request.DisputeEvidenceRequest;
import kg.freelance.dto.request.OpenDisputeRequest;
import kg.freelance.dto.response.DisputeEvidenceResponse;
import kg.freelance.dto.response.DisputeResponse;
import kg.freelance.entity.enums.DisputeStatus;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ForbiddenException;
import kg.freelance.exception.GlobalExceptionHandler;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.DisputeService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisputeController Tests")
class DisputeControllerTest {

    @Mock
    private DisputeService disputeService;

    @InjectMocks
    private DisputeController disputeController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(disputeController)
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

    private DisputeResponse buildDisputeResponse() {
        return DisputeResponse.builder()
                .id(1L).orderId(1L).orderTitle("Test Order")
                .openedById(1L).openedByName("Test User").openedByRole("CLIENT")
                .clientId(1L).clientName("Test User")
                .executorId(2L).executorName("Executor User")
                .reason("Quality issues").status(DisputeStatus.OPEN)
                .evidence(List.of()).evidenceCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/disputes/orders/{orderId}")
    class OpenDisputeTests {

        @Test
        @DisplayName("Should open dispute successfully")
        void shouldOpenDisputeSuccessfully() throws Exception {
            when(disputeService.openDispute(eq(1L), eq(1L), any(OpenDisputeRequest.class)))
                    .thenReturn(buildDisputeResponse());

            mockMvc.perform(post("/api/v1/disputes/orders/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"Quality is unacceptable and work is incomplete\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.orderId").value(1))
                    .andExpect(jsonPath("$.status").value("OPEN"));
        }

        @Test
        @DisplayName("Should return 400 when reason is blank")
        void shouldReturn400WhenReasonBlank() throws Exception {
            mockMvc.perform(post("/api/v1/disputes/orders/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when not participant")
        void shouldReturn403WhenNotParticipant() throws Exception {
            when(disputeService.openDispute(eq(1L), eq(1L), any(OpenDisputeRequest.class)))
                    .thenThrow(new ForbiddenException("Only order participants can open dispute"));

            mockMvc.perform(post("/api/v1/disputes/orders/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"Quality is unacceptable and work is incomplete\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 400 when dispute already exists")
        void shouldReturn400WhenDisputeExists() throws Exception {
            when(disputeService.openDispute(eq(1L), eq(1L), any(OpenDisputeRequest.class)))
                    .thenThrow(new BadRequestException("Dispute already exists for this order"));

            mockMvc.perform(post("/api/v1/disputes/orders/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"Quality is unacceptable and work is incomplete\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/disputes/orders/{orderId}")
    class GetDisputeByOrderTests {

        @Test
        @DisplayName("Should get dispute by order ID")
        void shouldGetDisputeByOrderId() throws Exception {
            when(disputeService.getDisputeByOrderId(1L, 1L)).thenReturn(buildDisputeResponse());

            mockMvc.perform(get("/api/v1/disputes/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(1))
                    .andExpect(jsonPath("$.reason").value("Quality issues"));
        }

        @Test
        @DisplayName("Should return 404 when dispute not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(disputeService.getDisputeByOrderId(999L, 1L))
                    .thenThrow(new ResourceNotFoundException("Dispute", "orderId", 999L));

            mockMvc.perform(get("/api/v1/disputes/orders/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/disputes/{id}")
    class GetDisputeByIdTests {

        @Test
        @DisplayName("Should get dispute by ID")
        void shouldGetDisputeById() throws Exception {
            when(disputeService.getDisputeById(1L, 1L)).thenReturn(buildDisputeResponse());

            mockMvc.perform(get("/api/v1/disputes/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("Should return 404 when dispute not found by ID")
        void shouldReturn404WhenNotFoundById() throws Exception {
            when(disputeService.getDisputeById(999L, 1L))
                    .thenThrow(new ResourceNotFoundException("Dispute", "id", 999L));

            mockMvc.perform(get("/api/v1/disputes/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/disputes/{id}/evidence")
    class AddEvidenceTests {

        @Test
        @DisplayName("Should add evidence successfully")
        void shouldAddEvidenceSuccessfully() throws Exception {
            DisputeEvidenceResponse evidenceResponse = DisputeEvidenceResponse.builder()
                    .id(1L).uploadedById(1L).uploadedByName("Test User").uploadedByRole("CLIENT")
                    .fileUrl("https://storage.example.com/file.pdf")
                    .fileName("evidence.pdf").fileType("application/pdf").fileSize(1024L)
                    .createdAt(LocalDateTime.now()).build();

            when(disputeService.addEvidence(eq(1L), eq(1L), any(DisputeEvidenceRequest.class)))
                    .thenReturn(evidenceResponse);

            DisputeEvidenceRequest request = new DisputeEvidenceRequest();
            request.setFileUrl("https://storage.example.com/file.pdf");
            request.setFileName("evidence.pdf");
            request.setFileType("application/pdf");
            request.setFileSize(1024L);
            request.setDescription("Payment receipt");

            mockMvc.perform(post("/api/v1/disputes/1/evidence")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.fileName").value("evidence.pdf"));
        }

        @Test
        @DisplayName("Should return 400 when fileUrl is missing")
        void shouldReturn400WhenFileUrlMissing() throws Exception {
            mockMvc.perform(post("/api/v1/disputes/1/evidence")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fileName\":\"file.pdf\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when dispute is resolved")
        void shouldReturn400WhenDisputeResolved() throws Exception {
            when(disputeService.addEvidence(eq(1L), eq(1L), any(DisputeEvidenceRequest.class)))
                    .thenThrow(new BadRequestException("Cannot add evidence to a resolved dispute"));

            DisputeEvidenceRequest request = new DisputeEvidenceRequest();
            request.setFileUrl("https://storage.example.com/file.pdf");
            request.setFileName("file.pdf");

            mockMvc.perform(post("/api/v1/disputes/1/evidence")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/disputes/{id}/evidence")
    class GetEvidenceTests {

        @Test
        @DisplayName("Should get evidence list")
        void shouldGetEvidenceList() throws Exception {
            DisputeEvidenceResponse evidence = DisputeEvidenceResponse.builder()
                    .id(1L).uploadedById(1L).uploadedByName("Test User").uploadedByRole("CLIENT")
                    .fileUrl("https://storage.example.com/file.pdf")
                    .fileName("evidence.pdf").fileType("application/pdf")
                    .createdAt(LocalDateTime.now()).build();

            when(disputeService.getEvidence(1L, 1L)).thenReturn(List.of(evidence));

            mockMvc.perform(get("/api/v1/disputes/1/evidence"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].fileName").value("evidence.pdf"));
        }

        @Test
        @DisplayName("Should return empty list when no evidence")
        void shouldReturnEmptyList() throws Exception {
            when(disputeService.getEvidence(1L, 1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/disputes/1/evidence"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }
}
