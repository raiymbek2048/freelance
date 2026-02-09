package kg.freelance.service.impl;

import kg.freelance.dto.request.DisputeEvidenceRequest;
import kg.freelance.dto.request.OpenDisputeRequest;
import kg.freelance.dto.request.ResolveDisputeRequest;
import kg.freelance.dto.response.*;
import kg.freelance.entity.*;
import kg.freelance.entity.enums.DisputeResolution;
import kg.freelance.entity.enums.DisputeStatus;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ForbiddenException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.*;
import kg.freelance.service.DisputeService;
import kg.freelance.service.EmailService;
import kg.freelance.service.InAppNotificationService;
import kg.freelance.entity.enums.NotificationType;
import kg.freelance.websocket.dto.WsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisputeServiceImpl implements DisputeService {

    private final DisputeRepository disputeRepository;
    private final DisputeEvidenceRepository disputeEvidenceRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ExecutorProfileRepository executorProfileRepository;
    private final MessageRepository messageRepository;
    private final EmailService emailService;
    private final InAppNotificationService inAppNotificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public DisputeResponse openDispute(Long userId, Long orderId, OpenDisputeRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        boolean isClient = order.getClient().getId().equals(userId);
        boolean isExecutor = order.getExecutor() != null && order.getExecutor().getId().equals(userId);

        if (!isClient && !isExecutor) {
            throw new ForbiddenException("Only order participants can open dispute");
        }

        if (order.getStatus() != OrderStatus.IN_PROGRESS && order.getStatus() != OrderStatus.ON_REVIEW) {
            throw new BadRequestException("Cannot open dispute in current status");
        }

        if (disputeRepository.existsByOrderId(orderId)) {
            throw new BadRequestException("Dispute already exists for this order");
        }

        // Change order status
        order.setStatus(OrderStatus.DISPUTED);
        orderRepository.save(order);

        // Update executor disputed orders count
        if (order.getExecutor() != null) {
            ExecutorProfile profile = executorProfileRepository.findById(order.getExecutor().getId()).orElse(null);
            if (profile != null) {
                profile.setDisputedOrders(profile.getDisputedOrders() + 1);
                executorProfileRepository.save(profile);
            }
        }

        // Find existing chat room for this order
        ChatRoom chatRoom = null;
        if (order.getExecutor() != null) {
            chatRoom = chatRoomRepository.findByOrderIdAndExecutorId(orderId, order.getExecutor().getId())
                    .orElse(null);
        }

        // Create dispute entity
        Dispute dispute = Dispute.builder()
                .order(order)
                .openedBy(isClient ? order.getClient() : order.getExecutor())
                .reason(request.getReason())
                .status(DisputeStatus.OPEN)
                .chatRoom(chatRoom)
                .build();
        dispute = disputeRepository.save(dispute);

        // Send system message in chat
        if (chatRoom != null) {
            User sender = isClient ? order.getClient() : order.getExecutor();
            User recipient = isClient ? order.getExecutor() : order.getClient();
            String initiatorRole = isClient ? "Заказчик" : "Исполнитель";

            String messageText = initiatorRole + " открыл спор по заказу \"" + order.getTitle() + "\"."
                    + "\n\nПричина: " + request.getReason()
                    + "\n\nМодератор рассмотрит ситуацию и примет решение.";

            Message systemMessage = Message.builder()
                    .chatRoom(chatRoom)
                    .sender(sender)
                    .content(messageText)
                    .isRead(false)
                    .build();
            systemMessage = messageRepository.save(systemMessage);

            chatRoom.setLastMessageAt(LocalDateTime.now());
            chatRoomRepository.save(chatRoom);

            WsMessage wsMessage = WsMessage.builder()
                    .id(systemMessage.getId())
                    .chatRoomId(chatRoom.getId())
                    .senderId(sender.getId())
                    .senderName(sender.getFullName())
                    .senderAvatarUrl(sender.getAvatarUrl())
                    .content(messageText)
                    .createdAt(systemMessage.getCreatedAt())
                    .type(WsMessage.MessageType.SYSTEM)
                    .build();

            messagingTemplate.convertAndSendToUser(
                    recipient.getEmail(),
                    "/queue/messages",
                    wsMessage
            );
        }

        // Notify admins via WebSocket
        notifyAdmins(order, request.getReason(), isClient ? "заказчиком" : "исполнителем");

        // Send email notifications (extract data before async call to avoid Hibernate session issues)
        String orderTitle = order.getTitle();
        String reason = request.getReason();

        User sender = isClient ? order.getClient() : order.getExecutor();
        User recipientUser = isClient ? order.getExecutor() : order.getClient();
        emailService.sendDisputeOpened(recipientUser.getEmail(), recipientUser.getFullName(), orderTitle, orderId, reason);
        emailService.sendDisputeOpened(sender.getEmail(), sender.getFullName(), orderTitle, orderId, reason);

        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        for (User admin : admins) {
            emailService.sendDisputeOpened(admin.getEmail(), admin.getFullName(), orderTitle, orderId, reason);
        }

        // In-app notification to the other party
        String initiatorRole = isClient ? "Заказчик" : "Исполнитель";
        inAppNotificationService.send(
                recipientUser,
                NotificationType.DISPUTE_OPENED,
                "Открыт спор",
                initiatorRole + " открыл спор по заказу \"" + orderTitle + "\". Причина: " + reason,
                order,
                "/orders/" + orderId
        );

        return mapToResponse(dispute);
    }

    @Override
    @Transactional(readOnly = true)
    public DisputeResponse getDisputeByOrderId(Long orderId, Long userId) {
        Dispute dispute = disputeRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "orderId", orderId));

        validateParticipant(dispute, userId);
        return mapToResponse(dispute);
    }

    @Override
    @Transactional(readOnly = true)
    public DisputeResponse getDisputeById(Long disputeId, Long userId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        validateParticipant(dispute, userId);
        return mapToResponse(dispute);
    }

    @Override
    @Transactional
    public DisputeEvidenceResponse addEvidence(Long disputeId, Long userId, DisputeEvidenceRequest request) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        validateParticipant(dispute, userId);

        if (dispute.getStatus() == DisputeStatus.RESOLVED) {
            throw new BadRequestException("Cannot add evidence to a resolved dispute");
        }

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        DisputeEvidence evidence = DisputeEvidence.builder()
                .dispute(dispute)
                .uploadedBy(uploader)
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .fileType(request.getFileType())
                .fileSize(request.getFileSize())
                .description(request.getDescription())
                .build();
        evidence = disputeEvidenceRepository.save(evidence);

        // Send system message about new evidence
        if (dispute.getChatRoom() != null) {
            String messageText = uploader.getFullName() + " загрузил(а) доказательство: " + request.getFileName();
            if (request.getDescription() != null && !request.getDescription().isBlank()) {
                messageText += "\nОписание: " + request.getDescription();
            }

            Message systemMessage = Message.builder()
                    .chatRoom(dispute.getChatRoom())
                    .sender(uploader)
                    .content(messageText)
                    .isRead(false)
                    .build();
            messageRepository.save(systemMessage);

            dispute.getChatRoom().setLastMessageAt(LocalDateTime.now());
            chatRoomRepository.save(dispute.getChatRoom());
        }

        return mapToEvidenceResponse(evidence, dispute.getOrder());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputeEvidenceResponse> getEvidence(Long disputeId, Long userId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        validateParticipant(dispute, userId);

        return disputeEvidenceRepository.findByDisputeIdOrderByCreatedAtAsc(disputeId).stream()
                .map(e -> mapToEvidenceResponse(e, dispute.getOrder()))
                .collect(Collectors.toList());
    }

    // ==================== Admin actions ====================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DisputeResponse> getActiveDisputes(Pageable pageable) {
        Page<Dispute> page = disputeRepository.findActiveDisputes(pageable);
        List<DisputeResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DisputeResponse> getAllDisputes(String status, Pageable pageable) {
        Page<Dispute> page;
        if (status != null && !status.isBlank()) {
            DisputeStatus disputeStatus = DisputeStatus.valueOf(status.toUpperCase());
            page = disputeRepository.findByStatusOrderByCreatedAtDesc(disputeStatus, pageable);
        } else {
            page = disputeRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        List<DisputeResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public DisputeResponse getDisputeForAdmin(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));
        return mapToResponse(dispute);
    }

    @Override
    @Transactional
    public DisputeResponse takeDispute(Long disputeId, Long adminId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        if (dispute.getStatus() == DisputeStatus.RESOLVED) {
            throw new BadRequestException("Dispute is already resolved");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        dispute.setAdmin(admin);
        dispute = disputeRepository.save(dispute);

        // Notify parties (extract data before async call to avoid Hibernate session issues)
        Order order = dispute.getOrder();
        String orderTitle = order.getTitle();
        Long orderId = order.getId();

        User client = order.getClient();
        emailService.sendDisputeUnderReview(client.getEmail(), client.getFullName(), orderTitle, orderId);
        if (order.getExecutor() != null) {
            User executor = order.getExecutor();
            emailService.sendDisputeUnderReview(executor.getEmail(), executor.getFullName(), orderTitle, orderId);
        }

        // Send system message in chat
        if (dispute.getChatRoom() != null) {
            String messageText = "Модератор " + admin.getFullName() + " взял спор на рассмотрение.";
            Message systemMessage = Message.builder()
                    .chatRoom(dispute.getChatRoom())
                    .sender(admin)
                    .content(messageText)
                    .isRead(false)
                    .build();
            messageRepository.save(systemMessage);
        }

        return mapToResponse(dispute);
    }

    @Override
    @Transactional
    public DisputeResponse addAdminNotes(Long disputeId, Long adminId, String notes) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        dispute.setAdminNotes(notes);
        dispute = disputeRepository.save(dispute);

        return mapToResponse(dispute);
    }

    @Override
    @Transactional
    public DisputeResponse resolveDispute(Long disputeId, Long adminId, ResolveDisputeRequest request) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        if (dispute.getStatus() == DisputeStatus.RESOLVED) {
            throw new BadRequestException("Dispute is already resolved");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        // Update dispute
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setAdmin(admin);
        dispute.setResolution(request.getFavorClient() ? DisputeResolution.FAVOR_CLIENT : DisputeResolution.FAVOR_EXECUTOR);
        dispute.setResolutionNotes(request.getResolutionNotes());
        dispute.setResolvedAt(LocalDateTime.now());
        if (request.getAdminNotes() != null) {
            dispute.setAdminNotes(request.getAdminNotes());
        }

        // Update order status
        Order order = dispute.getOrder();
        if (request.getFavorClient()) {
            order.setStatus(OrderStatus.CANCELLED);
        } else {
            order.setStatus(OrderStatus.COMPLETED);
            order.setCompletedAt(LocalDateTime.now());

            // Update executor completed orders
            if (order.getExecutor() != null) {
                ExecutorProfile profile = executorProfileRepository.findById(order.getExecutor().getId()).orElse(null);
                if (profile != null) {
                    profile.setCompletedOrders(profile.getCompletedOrders() + 1);
                    executorProfileRepository.save(profile);
                }
            }
        }
        orderRepository.save(order);
        dispute = disputeRepository.save(dispute);

        // Send system message in chat
        if (dispute.getChatRoom() != null) {
            String verdict = request.getFavorClient() ? "в пользу заказчика" : "в пользу исполнителя";
            String messageText = "Спор разрешён " + verdict + " модератором " + admin.getFullName() + ".";
            if (request.getResolutionNotes() != null && !request.getResolutionNotes().isBlank()) {
                messageText += "\nКомментарий: " + request.getResolutionNotes();
            }

            Message systemMessage = Message.builder()
                    .chatRoom(dispute.getChatRoom())
                    .sender(admin)
                    .content(messageText)
                    .isRead(false)
                    .build();
            messageRepository.save(systemMessage);
        }

        // Send email notifications (extract data before async call to avoid Hibernate session issues)
        String resolution = request.getFavorClient() ? "в пользу заказчика" : "в пользу исполнителя";
        String orderTitle = order.getTitle();
        Long orderId = order.getId();
        String resolutionNotes = request.getResolutionNotes();

        User client = order.getClient();
        emailService.sendDisputeResolved(client.getEmail(), client.getFullName(), orderTitle, orderId, resolution, resolutionNotes);
        if (order.getExecutor() != null) {
            User executor = order.getExecutor();
            emailService.sendDisputeResolved(executor.getEmail(), executor.getFullName(), orderTitle, orderId, resolution, resolutionNotes);
        }

        // In-app notification to both parties
        String notifMessage = "Спор по заказу \"" + orderTitle + "\" разрешён " + resolution + ".";
        inAppNotificationService.send(
                client,
                NotificationType.DISPUTE_RESOLVED,
                "Спор разрешён",
                notifMessage,
                order,
                "/orders/" + orderId
        );
        if (order.getExecutor() != null) {
            inAppNotificationService.send(
                    order.getExecutor(),
                    NotificationType.DISPUTE_RESOLVED,
                    "Спор разрешён",
                    notifMessage,
                    order,
                    "/orders/" + orderId
            );
        }

        return mapToResponse(dispute);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getDisputeMessages(Long disputeId, Pageable pageable) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        if (dispute.getChatRoom() == null) {
            return PageResponse.<MessageResponse>builder()
                    .content(List.of())
                    .page(0)
                    .size(pageable.getPageSize())
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build();
        }

        Page<Message> page = messageRepository.findByChatRoomIdOrderByCreatedAtDesc(
                dispute.getChatRoom().getId(), pageable);

        List<MessageResponse> content = page.getContent().stream()
                .map(m -> MessageResponse.builder()
                        .id(m.getId())
                        .chatRoomId(m.getChatRoom().getId())
                        .senderId(m.getSender().getId())
                        .senderName(m.getSender().getFullName())
                        .senderAvatarUrl(m.getSender().getAvatarUrl())
                        .content(m.getContent())
                        .attachments(m.getAttachments())
                        .isRead(m.getIsRead())
                        .isMine(false) // admin view
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
    }

    // ==================== Helpers ====================

    private void validateParticipant(Dispute dispute, Long userId) {
        Order order = dispute.getOrder();
        boolean isClient = order.getClient().getId().equals(userId);
        boolean isExecutor = order.getExecutor() != null && order.getExecutor().getId().equals(userId);

        if (!isClient && !isExecutor) {
            throw new ForbiddenException("Only order participants can access this dispute");
        }
    }

    private void notifyAdmins(Order order, String reason, String initiatedBy) {
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);

        for (User admin : admins) {
            WsMessage notification = WsMessage.builder()
                    .id(System.currentTimeMillis())
                    .chatRoomId(0L)
                    .senderId(0L)
                    .senderName("Система")
                    .content(String.format(
                            "Открыт новый спор по заказу #%d \"%s\"\nИнициирован: %s\n%s",
                            order.getId(),
                            order.getTitle(),
                            initiatedBy,
                            reason != null && !reason.isBlank() ? "Причина: " + reason : ""
                    ))
                    .createdAt(LocalDateTime.now())
                    .type(WsMessage.MessageType.ADMIN_NOTIFICATION)
                    .build();

            messagingTemplate.convertAndSendToUser(
                    admin.getEmail(),
                    "/queue/admin-notifications",
                    notification
            );
        }
    }

    private DisputeResponse mapToResponse(Dispute dispute) {
        Order order = dispute.getOrder();
        User openedBy = dispute.getOpenedBy();
        boolean openedByClient = order.getClient().getId().equals(openedBy.getId());

        DisputeResponse.DisputeResponseBuilder builder = DisputeResponse.builder()
                .id(dispute.getId())
                .orderId(order.getId())
                .orderTitle(order.getTitle())
                .openedById(openedBy.getId())
                .openedByName(openedBy.getFullName())
                .openedByRole(openedByClient ? "CLIENT" : "EXECUTOR")
                .clientId(order.getClient().getId())
                .clientName(order.getClient().getFullName())
                .clientEmail(order.getClient().getEmail())
                .clientAvatarUrl(order.getClient().getAvatarUrl())
                .reason(dispute.getReason())
                .status(dispute.getStatus())
                .chatRoomId(dispute.getChatRoom() != null ? dispute.getChatRoom().getId() : null)
                .evidenceCount(dispute.getEvidence().size())
                .createdAt(dispute.getCreatedAt())
                .updatedAt(dispute.getUpdatedAt())
                .resolvedAt(dispute.getResolvedAt());

        if (order.getExecutor() != null) {
            builder.executorId(order.getExecutor().getId())
                    .executorName(order.getExecutor().getFullName())
                    .executorEmail(order.getExecutor().getEmail())
                    .executorAvatarUrl(order.getExecutor().getAvatarUrl());
        }

        if (dispute.getAdmin() != null) {
            builder.adminId(dispute.getAdmin().getId())
                    .adminName(dispute.getAdmin().getFullName());
        }
        builder.adminNotes(dispute.getAdminNotes());
        builder.resolution(dispute.getResolution());
        builder.resolutionNotes(dispute.getResolutionNotes());

        List<DisputeEvidenceResponse> evidenceList = dispute.getEvidence().stream()
                .map(e -> mapToEvidenceResponse(e, order))
                .collect(Collectors.toList());
        builder.evidence(evidenceList);

        return builder.build();
    }

    private DisputeEvidenceResponse mapToEvidenceResponse(DisputeEvidence evidence, Order order) {
        User uploader = evidence.getUploadedBy();
        boolean isClient = order.getClient().getId().equals(uploader.getId());

        return DisputeEvidenceResponse.builder()
                .id(evidence.getId())
                .uploadedById(uploader.getId())
                .uploadedByName(uploader.getFullName())
                .uploadedByRole(isClient ? "CLIENT" : "EXECUTOR")
                .fileUrl(evidence.getFileUrl())
                .fileName(evidence.getFileName())
                .fileType(evidence.getFileType())
                .fileSize(evidence.getFileSize())
                .description(evidence.getDescription())
                .createdAt(evidence.getCreatedAt())
                .build();
    }
}
