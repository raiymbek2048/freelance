package kg.freelance.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kg.freelance.dto.request.ResolveDisputeRequest;
import kg.freelance.dto.response.DisputeResponse;
import kg.freelance.dto.response.MessageResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.entity.enums.DisputeResolution;
import kg.freelance.entity.enums.DisputeStatus;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.BadRequestException;
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
@DisplayName("AdminDisputeController Tests")
class AdminDisputeControllerTest {

    @Mock
    private DisputeService disputeService;

    @InjectMocks
    private AdminDisputeController adminDisputeController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminDisputeController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        adminPrincipal = UserPrincipal.builder()
                .id(1L).email("admin@example.com").fullName("Admin User")
                .role(UserRole.ADMIN).active(true).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities()));
    }

    private DisputeResponse buildDisputeResponse(DisputeStatus status) {
        return DisputeResponse.builder()
                .id(1L).orderId(1L).orderTitle("Test Order")
                .openedById(2L).openedByName("Client User").openedByRole("CLIENT")
                .clientId(2L).clientName("Client User")
                .executorId(3L).executorName("Executor User")
                .reason("Quality issues").status(status)
                .evidence(List.of()).evidenceCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/admin/disputes")
    class GetAllDisputesTests {

        @Test
        @DisplayName("Should get all disputes")
        void shouldGetAllDisputes() throws Exception {
            PageResponse<DisputeResponse> page = PageResponse.<DisputeResponse>builder()
                    .content(List.of(buildDisputeResponse(DisputeStatus.OPEN)))
                    .page(0).size(20).totalElements(1).totalPages(1).first(true).last(true).build();

            when(disputeService.getAllDisputes(isNull(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/disputes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should filter disputes by status")
        void shouldFilterByStatus() throws Exception {
            PageResponse<DisputeResponse> page = PageResponse.<DisputeResponse>builder()
                    .content(List.of(buildDisputeResponse(DisputeStatus.OPEN)))
                    .page(0).size(20).totalElements(1).totalPages(1).first(true).last(true).build();

            when(disputeService.getAllDisputes(eq("OPEN"), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/disputes")
                            .param("status", "OPEN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("OPEN"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/disputes/active")
    class GetActiveDisputesTests {

        @Test
        @DisplayName("Should get active disputes")
        void shouldGetActiveDisputes() throws Exception {
            PageResponse<DisputeResponse> page = PageResponse.<DisputeResponse>builder()
                    .content(List.of(buildDisputeResponse(DisputeStatus.OPEN)))
                    .page(0).size(20).totalElements(1).totalPages(1).first(true).last(true).build();

            when(disputeService.getActiveDisputes(any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/disputes/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("OPEN"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/disputes/{id}")
    class GetDisputeDetailTests {

        @Test
        @DisplayName("Should get dispute detail")
        void shouldGetDisputeDetail() throws Exception {
            when(disputeService.getDisputeForAdmin(1L)).thenReturn(buildDisputeResponse(DisputeStatus.OPEN));

            mockMvc.perform(get("/api/v1/admin/disputes/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.reason").value("Quality issues"));
        }

        @Test
        @DisplayName("Should return 404 when dispute not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(disputeService.getDisputeForAdmin(999L))
                    .thenThrow(new ResourceNotFoundException("Dispute", "id", 999L));

            mockMvc.perform(get("/api/v1/admin/disputes/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/disputes/{id}/take")
    class TakeDisputeTests {

        @Test
        @DisplayName("Should take dispute for review")
        void shouldTakeDisputeForReview() throws Exception {
            DisputeResponse response = buildDisputeResponse(DisputeStatus.UNDER_REVIEW);

            when(disputeService.takeDispute(1L, 1L)).thenReturn(response);

            mockMvc.perform(put("/api/v1/admin/disputes/1/take"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
        }

        @Test
        @DisplayName("Should return 400 when dispute already resolved")
        void shouldReturn400WhenAlreadyResolved() throws Exception {
            when(disputeService.takeDispute(1L, 1L))
                    .thenThrow(new BadRequestException("Dispute is already resolved"));

            mockMvc.perform(put("/api/v1/admin/disputes/1/take"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/disputes/{id}/notes")
    class AddNotesTests {

        @Test
        @DisplayName("Should add admin notes")
        void shouldAddAdminNotes() throws Exception {
            DisputeResponse response = buildDisputeResponse(DisputeStatus.UNDER_REVIEW);

            when(disputeService.addAdminNotes(eq(1L), eq(1L), eq("Investigation notes")))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/admin/disputes/1/notes")
                            .param("notes", "Investigation notes"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/disputes/{id}/resolve")
    class ResolveDisputeTests {

        @Test
        @DisplayName("Should resolve dispute in favor of client")
        void shouldResolveInFavorOfClient() throws Exception {
            DisputeResponse response = DisputeResponse.builder()
                    .id(1L).orderId(1L).orderTitle("Test Order")
                    .openedById(2L).openedByName("Client User").openedByRole("CLIENT")
                    .clientId(2L).clientName("Client User")
                    .executorId(3L).executorName("Executor User")
                    .reason("Quality issues").status(DisputeStatus.RESOLVED)
                    .resolution(DisputeResolution.FAVOR_CLIENT)
                    .resolutionNotes("Client's claim is valid")
                    .evidence(List.of()).evidenceCount(0)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .resolvedAt(LocalDateTime.now())
                    .build();

            when(disputeService.resolveDispute(eq(1L), eq(1L), any(ResolveDisputeRequest.class)))
                    .thenReturn(response);

            ResolveDisputeRequest request = new ResolveDisputeRequest();
            request.setFavorClient(true);
            request.setResolutionNotes("Client's claim is valid");

            mockMvc.perform(put("/api/v1/admin/disputes/1/resolve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESOLVED"))
                    .andExpect(jsonPath("$.resolution").value("FAVOR_CLIENT"));
        }

        @Test
        @DisplayName("Should resolve dispute in favor of executor")
        void shouldResolveInFavorOfExecutor() throws Exception {
            DisputeResponse response = DisputeResponse.builder()
                    .id(1L).orderId(1L).orderTitle("Test Order")
                    .openedById(2L).openedByName("Client User").openedByRole("CLIENT")
                    .clientId(2L).clientName("Client User")
                    .executorId(3L).executorName("Executor User")
                    .reason("Quality issues").status(DisputeStatus.RESOLVED)
                    .resolution(DisputeResolution.FAVOR_EXECUTOR)
                    .evidence(List.of()).evidenceCount(0)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .resolvedAt(LocalDateTime.now())
                    .build();

            when(disputeService.resolveDispute(eq(1L), eq(1L), any(ResolveDisputeRequest.class)))
                    .thenReturn(response);

            ResolveDisputeRequest request = new ResolveDisputeRequest();
            request.setFavorClient(false);

            mockMvc.perform(put("/api/v1/admin/disputes/1/resolve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resolution").value("FAVOR_EXECUTOR"));
        }

        @Test
        @DisplayName("Should return 400 when favorClient is missing")
        void shouldReturn400WhenFavorClientMissing() throws Exception {
            mockMvc.perform(put("/api/v1/admin/disputes/1/resolve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"resolutionNotes\":\"Some notes\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when dispute already resolved")
        void shouldReturn400WhenAlreadyResolved() throws Exception {
            when(disputeService.resolveDispute(eq(1L), eq(1L), any(ResolveDisputeRequest.class)))
                    .thenThrow(new BadRequestException("Dispute is already resolved"));

            ResolveDisputeRequest request = new ResolveDisputeRequest();
            request.setFavorClient(true);

            mockMvc.perform(put("/api/v1/admin/disputes/1/resolve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/disputes/{id}/messages")
    class GetDisputeMessagesTests {

        @Test
        @DisplayName("Should get dispute messages")
        void shouldGetDisputeMessages() throws Exception {
            MessageResponse msg = MessageResponse.builder()
                    .id(1L).chatRoomId(1L).senderId(2L).senderName("Client User")
                    .content("Test message").isRead(false).isMine(false)
                    .createdAt(LocalDateTime.now()).build();

            PageResponse<MessageResponse> page = PageResponse.<MessageResponse>builder()
                    .content(List.of(msg)).page(0).size(100)
                    .totalElements(1).totalPages(1).first(true).last(true).build();

            when(disputeService.getDisputeMessages(eq(1L), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/disputes/1/messages"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].content").value("Test message"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should return empty when no messages")
        void shouldReturnEmptyWhenNoMessages() throws Exception {
            PageResponse<MessageResponse> page = PageResponse.<MessageResponse>builder()
                    .content(List.of()).page(0).size(100)
                    .totalElements(0).totalPages(0).first(true).last(true).build();

            when(disputeService.getDisputeMessages(eq(1L), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/disputes/1/messages"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }
}
