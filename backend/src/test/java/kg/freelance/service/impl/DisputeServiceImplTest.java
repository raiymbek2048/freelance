package kg.freelance.service.impl;

import kg.freelance.dto.request.DisputeEvidenceRequest;
import kg.freelance.dto.request.OpenDisputeRequest;
import kg.freelance.dto.request.ResolveDisputeRequest;
import kg.freelance.dto.response.DisputeEvidenceResponse;
import kg.freelance.dto.response.DisputeResponse;
import kg.freelance.dto.response.MessageResponse;
import kg.freelance.dto.response.PageResponse;
import kg.freelance.entity.*;
import kg.freelance.entity.enums.DisputeResolution;
import kg.freelance.entity.enums.DisputeStatus;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ForbiddenException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.*;
import kg.freelance.service.EmailService;
import kg.freelance.service.InAppNotificationService;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisputeService Tests")
class DisputeServiceImplTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private DisputeEvidenceRepository disputeEvidenceRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ExecutorProfileRepository executorProfileRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private InAppNotificationService inAppNotificationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private DisputeServiceImpl disputeService;

    private User client;
    private User executor;
    private User admin;
    private Order order;
    private ExecutorProfile executorProfile;
    private ChatRoom chatRoom;
    private Dispute dispute;

    @BeforeEach
    void setUp() {
        client = User.builder().id(1L).email("client@test.com").fullName("Client User")
                .role(UserRole.USER).active(true).build();
        executor = User.builder().id(2L).email("executor@test.com").fullName("Executor User")
                .role(UserRole.USER).active(true).build();
        admin = User.builder().id(3L).email("admin@test.com").fullName("Admin User")
                .role(UserRole.ADMIN).active(true).build();

        order = Order.builder().id(1L).title("Test Order").description("Description")
                .client(client).executor(executor).status(OrderStatus.IN_PROGRESS).build();

        executorProfile = ExecutorProfile.builder()
                .id(2L).user(executor).completedOrders(5).disputedOrders(0).build();

        chatRoom = ChatRoom.builder().id(1L).order(order).client(client).executor(executor)
                .lastMessageAt(LocalDateTime.now()).build();

        dispute = Dispute.builder().id(1L).order(order).openedBy(client)
                .reason("Quality issues").status(DisputeStatus.OPEN)
                .chatRoom(chatRoom).evidence(new ArrayList<>()).build();
    }

    @Nested
    @DisplayName("Open Dispute Tests")
    class OpenDisputeTests {

        @Test
        @DisplayName("Should open dispute as client successfully")
        void shouldOpenDisputeAsClient() {
            OpenDisputeRequest request = new OpenDisputeRequest("Quality is unacceptable, work is incomplete");

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(disputeRepository.existsByOrderId(1L)).thenReturn(false);
            when(executorProfileRepository.findById(2L)).thenReturn(Optional.of(executorProfile));
            when(chatRoomRepository.findByOrderIdAndExecutorId(1L, 2L)).thenReturn(Optional.of(chatRoom));
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> {
                Dispute d = inv.getArgument(0);
                d.setId(1L);
                d.setEvidence(new ArrayList<>());
                return d;
            });
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(1L);
                m.setCreatedAt(LocalDateTime.now());
                return m;
            });
            when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(admin));

            DisputeResponse response = disputeService.openDispute(1L, 1L, request);

            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(DisputeStatus.OPEN);
            assertThat(response.getOpenedByRole()).isEqualTo("CLIENT");

            verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.DISPUTED));
            verify(executorProfileRepository).save(argThat(p -> p.getDisputedOrders() == 1));
            verify(disputeRepository).save(any(Dispute.class));
            verify(emailService, times(3)).sendDisputeOpened(anyString(), anyString(), anyString(), anyLong(), anyString());
        }

        @Test
        @DisplayName("Should open dispute as executor successfully")
        void shouldOpenDisputeAsExecutor() {
            OpenDisputeRequest request = new OpenDisputeRequest("Client not responding to requests");

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(disputeRepository.existsByOrderId(1L)).thenReturn(false);
            when(executorProfileRepository.findById(2L)).thenReturn(Optional.of(executorProfile));
            when(chatRoomRepository.findByOrderIdAndExecutorId(1L, 2L)).thenReturn(Optional.of(chatRoom));
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> {
                Dispute d = inv.getArgument(0);
                d.setId(1L);
                d.setEvidence(new ArrayList<>());
                return d;
            });
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(1L);
                m.setCreatedAt(LocalDateTime.now());
                return m;
            });
            when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(admin));

            DisputeResponse response = disputeService.openDispute(2L, 1L, request);

            assertThat(response).isNotNull();
            assertThat(response.getOpenedByRole()).isEqualTo("EXECUTOR");
            verify(disputeRepository).save(any(Dispute.class));
        }

        @Test
        @DisplayName("Should throw when user is not a participant")
        void shouldThrowWhenNotParticipant() {
            OpenDisputeRequest request = new OpenDisputeRequest("Some reason for dispute");
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> disputeService.openDispute(99L, 1L, request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Only order participants");
        }

        @Test
        @DisplayName("Should throw when order is not in progress")
        void shouldThrowWhenOrderNotInProgress() {
            order.setStatus(OrderStatus.NEW);
            OpenDisputeRequest request = new OpenDisputeRequest("Some reason for dispute");
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> disputeService.openDispute(1L, 1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cannot open dispute");
        }

        @Test
        @DisplayName("Should allow dispute when order is ON_REVIEW")
        void shouldAllowDisputeWhenOnReview() {
            order.setStatus(OrderStatus.ON_REVIEW);
            OpenDisputeRequest request = new OpenDisputeRequest("Executor submitted incomplete work");

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(disputeRepository.existsByOrderId(1L)).thenReturn(false);
            when(executorProfileRepository.findById(2L)).thenReturn(Optional.of(executorProfile));
            when(chatRoomRepository.findByOrderIdAndExecutorId(1L, 2L)).thenReturn(Optional.of(chatRoom));
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> {
                Dispute d = inv.getArgument(0);
                d.setId(1L);
                d.setEvidence(new ArrayList<>());
                return d;
            });
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(1L);
                m.setCreatedAt(LocalDateTime.now());
                return m;
            });
            when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(admin));

            DisputeResponse response = disputeService.openDispute(1L, 1L, request);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should throw when dispute already exists")
        void shouldThrowWhenDisputeAlreadyExists() {
            OpenDisputeRequest request = new OpenDisputeRequest("Some reason for dispute");
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(disputeRepository.existsByOrderId(1L)).thenReturn(true);

            assertThatThrownBy(() -> disputeService.openDispute(1L, 1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should throw when order not found")
        void shouldThrowWhenOrderNotFound() {
            OpenDisputeRequest request = new OpenDisputeRequest("Some reason for dispute");
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.openDispute(1L, 999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Dispute Tests")
    class GetDisputeTests {

        @Test
        @DisplayName("Should get dispute by order ID as participant")
        void shouldGetDisputeByOrderId() {
            when(disputeRepository.findByOrderId(1L)).thenReturn(Optional.of(dispute));

            DisputeResponse response = disputeService.getDisputeByOrderId(1L, 1L);

            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw when non-participant tries to get dispute")
        void shouldThrowWhenNonParticipantGetsDispute() {
            when(disputeRepository.findByOrderId(1L)).thenReturn(Optional.of(dispute));

            assertThatThrownBy(() -> disputeService.getDisputeByOrderId(1L, 99L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Should get dispute by ID as participant")
        void shouldGetDisputeById() {
            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

            DisputeResponse response = disputeService.getDisputeById(1L, 1L);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw when dispute not found by order ID")
        void shouldThrowWhenDisputeNotFoundByOrderId() {
            when(disputeRepository.findByOrderId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.getDisputeByOrderId(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Add Evidence Tests")
    class AddEvidenceTests {

        @Test
        @DisplayName("Should add evidence as client")
        void shouldAddEvidenceAsClient() {
            DisputeEvidenceRequest request = new DisputeEvidenceRequest();
            request.setFileUrl("https://storage.example.com/evidence/file.pdf");
            request.setFileName("evidence.pdf");
            request.setFileType("application/pdf");
            request.setFileSize(1024L);
            request.setDescription("Payment receipt");

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
            when(userRepository.findById(1L)).thenReturn(Optional.of(client));
            when(disputeEvidenceRepository.save(any(DisputeEvidence.class))).thenAnswer(inv -> {
                DisputeEvidence e = inv.getArgument(0);
                e.setId(1L);
                e.setCreatedAt(LocalDateTime.now());
                return e;
            });
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(1L);
                return m;
            });

            DisputeEvidenceResponse response = disputeService.addEvidence(1L, 1L, request);

            assertThat(response).isNotNull();
            assertThat(response.getFileName()).isEqualTo("evidence.pdf");
            assertThat(response.getUploadedByRole()).isEqualTo("CLIENT");
            verify(disputeEvidenceRepository).save(any(DisputeEvidence.class));
        }

        @Test
        @DisplayName("Should add evidence as executor")
        void shouldAddEvidenceAsExecutor() {
            DisputeEvidenceRequest request = new DisputeEvidenceRequest();
            request.setFileUrl("https://storage.example.com/evidence/screen.png");
            request.setFileName("screenshot.png");
            request.setFileType("image/png");
            request.setFileSize(2048L);

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
            when(userRepository.findById(2L)).thenReturn(Optional.of(executor));
            when(disputeEvidenceRepository.save(any(DisputeEvidence.class))).thenAnswer(inv -> {
                DisputeEvidence e = inv.getArgument(0);
                e.setId(2L);
                e.setCreatedAt(LocalDateTime.now());
                return e;
            });
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(2L);
                return m;
            });

            DisputeEvidenceResponse response = disputeService.addEvidence(1L, 2L, request);

            assertThat(response).isNotNull();
            assertThat(response.getUploadedByRole()).isEqualTo("EXECUTOR");
        }

        @Test
        @DisplayName("Should throw when dispute is resolved")
        void shouldThrowWhenDisputeResolved() {
            dispute.setStatus(DisputeStatus.RESOLVED);
            DisputeEvidenceRequest request = new DisputeEvidenceRequest();
            request.setFileUrl("https://storage.example.com/evidence/file.pdf");
            request.setFileName("file.pdf");

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

            assertThatThrownBy(() -> disputeService.addEvidence(1L, 1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("resolved");
        }

        @Test
        @DisplayName("Should throw when non-participant adds evidence")
        void shouldThrowWhenNonParticipantAddsEvidence() {
            DisputeEvidenceRequest request = new DisputeEvidenceRequest();
            request.setFileUrl("https://storage.example.com/evidence/file.pdf");
            request.setFileName("file.pdf");

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

            assertThatThrownBy(() -> disputeService.addEvidence(1L, 99L, request))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Should allow evidence when dispute is under review")
        void shouldAllowEvidenceWhenUnderReview() {
            dispute.setStatus(DisputeStatus.UNDER_REVIEW);
            DisputeEvidenceRequest request = new DisputeEvidenceRequest();
            request.setFileUrl("https://storage.example.com/evidence/file.pdf");
            request.setFileName("additional_evidence.pdf");

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
            when(userRepository.findById(1L)).thenReturn(Optional.of(client));
            when(disputeEvidenceRepository.save(any(DisputeEvidence.class))).thenAnswer(inv -> {
                DisputeEvidence e = inv.getArgument(0);
                e.setId(1L);
                e.setCreatedAt(LocalDateTime.now());
                return e;
            });
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(1L);
                return m;
            });

            DisputeEvidenceResponse response = disputeService.addEvidence(1L, 1L, request);
            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("Get Evidence Tests")
    class GetEvidenceTests {

        @Test
        @DisplayName("Should return evidence list for participant")
        void shouldReturnEvidenceList() {
            DisputeEvidence evidence = DisputeEvidence.builder()
                    .id(1L).dispute(dispute).uploadedBy(client)
                    .fileUrl("https://storage.example.com/file.pdf")
                    .fileName("file.pdf").fileType("application/pdf")
                    .createdAt(LocalDateTime.now()).build();

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
            when(disputeEvidenceRepository.findByDisputeIdOrderByCreatedAtAsc(1L))
                    .thenReturn(List.of(evidence));

            List<DisputeEvidenceResponse> result = disputeService.getEvidence(1L, 1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFileName()).isEqualTo("file.pdf");
        }
    }

    @Nested
    @DisplayName("Take Dispute Tests")
    class TakeDisputeTests {

        @Test
        @DisplayName("Should take dispute for review")
        void shouldTakeDisputeForReview() {
            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
            when(userRepository.findById(3L)).thenReturn(Optional.of(admin));
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(1L);
                return m;
            });

            DisputeResponse response = disputeService.takeDispute(1L, 3L);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(DisputeStatus.UNDER_REVIEW);
            assertThat(response.getAdminName()).isEqualTo("Admin User");
            verify(emailService).sendDisputeUnderReview(eq(client.getEmail()), eq(client.getFullName()), eq(order.getTitle()), eq(order.getId()));
            verify(emailService).sendDisputeUnderReview(eq(executor.getEmail()), eq(executor.getFullName()), eq(order.getTitle()), eq(order.getId()));
        }

        @Test
        @DisplayName("Should throw when taking resolved dispute")
        void shouldThrowWhenTakingResolvedDispute() {
            dispute.setStatus(DisputeStatus.RESOLVED);
            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

            assertThatThrownBy(() -> disputeService.takeDispute(1L, 3L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already resolved");
        }

        @Test
        @DisplayName("Should throw when dispute not found")
        void shouldThrowWhenDisputeNotFoundForTake() {
            when(disputeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.takeDispute(999L, 3L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Add Admin Notes Tests")
    class AddAdminNotesTests {

        @Test
        @DisplayName("Should add admin notes")
        void shouldAddAdminNotes() {
            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

            DisputeResponse response = disputeService.addAdminNotes(1L, 3L, "Investigating the issue");

            assertThat(response).isNotNull();
            verify(disputeRepository).save(argThat(d -> "Investigating the issue".equals(d.getAdminNotes())));
        }
    }

    @Nested
    @DisplayName("Resolve Dispute Tests")
    class ResolveDisputeTests {

        @Test
        @DisplayName("Should resolve dispute in favor of client")
        void shouldResolveInFavorOfClient() {
            ResolveDisputeRequest request = new ResolveDisputeRequest();
            request.setFavorClient(true);
            request.setResolutionNotes("Client's claim is valid");

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
            when(userRepository.findById(3L)).thenReturn(Optional.of(admin));
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(1L);
                return m;
            });

            DisputeResponse response = disputeService.resolveDispute(1L, 3L, request);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(DisputeStatus.RESOLVED);
            assertThat(response.getResolution()).isEqualTo(DisputeResolution.FAVOR_CLIENT);
            verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CANCELLED));
            verify(emailService).sendDisputeResolved(eq(client.getEmail()), eq(client.getFullName()), eq(order.getTitle()), eq(order.getId()), contains("заказчика"), any());
            verify(emailService).sendDisputeResolved(eq(executor.getEmail()), eq(executor.getFullName()), eq(order.getTitle()), eq(order.getId()), contains("заказчика"), any());
        }

        @Test
        @DisplayName("Should resolve dispute in favor of executor")
        void shouldResolveInFavorOfExecutor() {
            ResolveDisputeRequest request = new ResolveDisputeRequest();
            request.setFavorClient(false);
            request.setResolutionNotes("Executor fulfilled requirements");

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
            when(userRepository.findById(3L)).thenReturn(Optional.of(admin));
            when(executorProfileRepository.findById(2L)).thenReturn(Optional.of(executorProfile));
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(1L);
                return m;
            });

            DisputeResponse response = disputeService.resolveDispute(1L, 3L, request);

            assertThat(response).isNotNull();
            assertThat(response.getResolution()).isEqualTo(DisputeResolution.FAVOR_EXECUTOR);
            verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.COMPLETED));
            verify(executorProfileRepository).save(argThat(p -> p.getCompletedOrders() == 6));
        }

        @Test
        @DisplayName("Should throw when resolving already resolved dispute")
        void shouldThrowWhenAlreadyResolved() {
            dispute.setStatus(DisputeStatus.RESOLVED);
            ResolveDisputeRequest request = new ResolveDisputeRequest();
            request.setFavorClient(true);

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

            assertThatThrownBy(() -> disputeService.resolveDispute(1L, 3L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already resolved");
        }

        @Test
        @DisplayName("Should set admin notes on resolve")
        void shouldSetAdminNotesOnResolve() {
            ResolveDisputeRequest request = new ResolveDisputeRequest();
            request.setFavorClient(true);
            request.setAdminNotes("Internal note: case reviewed");
            request.setResolutionNotes("Decision explanation");

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
            when(userRepository.findById(3L)).thenReturn(Optional.of(admin));
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(1L);
                return m;
            });

            disputeService.resolveDispute(1L, 3L, request);

            verify(disputeRepository).save(argThat(d ->
                    "Internal note: case reviewed".equals(d.getAdminNotes()) &&
                    "Decision explanation".equals(d.getResolutionNotes())));
        }
    }

    @Nested
    @DisplayName("Admin List Disputes Tests")
    class AdminListDisputesTests {

        @Test
        @DisplayName("Should get active disputes")
        void shouldGetActiveDisputes() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Dispute> page = new PageImpl<>(List.of(dispute), pageable, 1);
            when(disputeRepository.findActiveDisputes(pageable)).thenReturn(page);

            PageResponse<DisputeResponse> response = disputeService.getActiveDisputes(pageable);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get all disputes without filter")
        void shouldGetAllDisputesWithoutFilter() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Dispute> page = new PageImpl<>(List.of(dispute), pageable, 1);
            when(disputeRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);

            PageResponse<DisputeResponse> response = disputeService.getAllDisputes(null, pageable);

            assertThat(response.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should get disputes filtered by status")
        void shouldGetDisputesFilteredByStatus() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Dispute> page = new PageImpl<>(List.of(dispute), pageable, 1);
            when(disputeRepository.findByStatusOrderByCreatedAtDesc(DisputeStatus.OPEN, pageable))
                    .thenReturn(page);

            PageResponse<DisputeResponse> response = disputeService.getAllDisputes("OPEN", pageable);

            assertThat(response.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should get dispute for admin (no participant check)")
        void shouldGetDisputeForAdmin() {
            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

            DisputeResponse response = disputeService.getDisputeForAdmin(1L);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Get Dispute Messages Tests")
    class GetDisputeMessagesTests {

        @Test
        @DisplayName("Should return chat messages for dispute")
        void shouldReturnChatMessages() {
            Message msg = Message.builder().id(1L).chatRoom(chatRoom).sender(client)
                    .content("Test message").isRead(false)
                    .createdAt(LocalDateTime.now()).build();

            Pageable pageable = PageRequest.of(0, 100);
            Page<Message> page = new PageImpl<>(List.of(msg), pageable, 1);

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
            when(messageRepository.findByChatRoomIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);

            PageResponse<MessageResponse> response = disputeService.getDisputeMessages(1L, pageable);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getContent()).isEqualTo("Test message");
        }

        @Test
        @DisplayName("Should return empty when no chat room")
        void shouldReturnEmptyWhenNoChatRoom() {
            dispute.setChatRoom(null);
            Pageable pageable = PageRequest.of(0, 100);

            when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

            PageResponse<MessageResponse> response = disputeService.getDisputeMessages(1L, pageable);

            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isEqualTo(0);
        }
    }
}
