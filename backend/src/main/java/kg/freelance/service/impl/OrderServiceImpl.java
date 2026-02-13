package kg.freelance.service.impl;

import kg.freelance.dto.request.*;
import kg.freelance.dto.response.*;
import kg.freelance.entity.*;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ForbiddenException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.*;
import kg.freelance.dto.request.OpenDisputeRequest;
import kg.freelance.service.ChatService;
import kg.freelance.service.DisputeService;
import kg.freelance.service.EmailService;
import kg.freelance.service.ExecutorVerificationService;
import kg.freelance.service.InAppNotificationService;
import kg.freelance.service.OrderService;
import kg.freelance.service.SubscriptionService;
import kg.freelance.entity.enums.NotificationType;
import kg.freelance.websocket.dto.WsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderResponseRepository orderResponseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ExecutorProfileRepository executorProfileRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final ExecutorVerificationService executorVerificationService;
    private final EmailService emailService;
    private final ChatService chatService;
    private final SubscriptionService subscriptionService;
    private final DisputeService disputeService;
    private final InAppNotificationService inAppNotificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderListResponse> getPublicOrders(
            Long categoryId, BigDecimal budgetMin, BigDecimal budgetMax,
            String search, String location, Long userId, Pageable pageable) {

        Page<Order> page = orderRepository.findPublicOrders(categoryId, budgetMin, budgetMax, search, location, pageable);

        List<OrderListResponse> content = page.getContent().stream()
                .map(order -> mapToListResponseWithUserContext(order, userId))
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional
    public OrderDetailResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Increment view count only for non-owners
        boolean isOwner = userId != null && userId.equals(order.getClient().getId());
        boolean isExecutor = userId != null && order.getExecutor() != null && userId.equals(order.getExecutor().getId());
        if (!isOwner && !isExecutor) {
            orderRepository.incrementViewCount(orderId);
        }

        return mapToDetailResponse(order, userId);
    }

    @Override
    @Transactional
    public OrderDetailResponse createOrder(Long clientId, OrderCreateRequest request) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", clientId));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Order order = Order.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(category)
                .client(client)
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .deadline(request.getDeadline())
                .location(request.getLocation())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .attachments(request.getAttachments())
                .status(OrderStatus.NEW)
                .viewCount(0)
                .responseCount(0)
                .build();

        order = orderRepository.save(order);
        return mapToDetailResponse(order, clientId);
    }

    @Override
    @Transactional
    public OrderDetailResponse updateOrder(Long clientId, Long orderId, OrderUpdateRequest request) {
        Order order = getOrderAndValidateClient(orderId, clientId);

        if (order.getStatus() != OrderStatus.NEW) {
            throw new BadRequestException("Can only update orders in NEW status");
        }

        if (request.getTitle() != null) order.setTitle(request.getTitle());
        if (request.getDescription() != null) order.setDescription(request.getDescription());
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            order.setCategory(category);
        }
        if (request.getBudgetMin() != null) order.setBudgetMin(request.getBudgetMin());
        if (request.getBudgetMax() != null) order.setBudgetMax(request.getBudgetMax());
        if (request.getDeadline() != null) order.setDeadline(request.getDeadline());
        if (request.getIsPublic() != null) order.setIsPublic(request.getIsPublic());
        if (request.getAttachments() != null) order.setAttachments(request.getAttachments());

        order = orderRepository.save(order);
        return mapToDetailResponse(order, clientId);
    }

    @Override
    @Transactional
    public void deleteOrder(Long clientId, Long orderId) {
        Order order = getOrderAndValidateClient(orderId, clientId);

        if (order.getStatus() != OrderStatus.NEW) {
            throw new BadRequestException("Can only delete orders in NEW status");
        }

        orderRepository.delete(order);
    }

    @Override
    @Transactional
    public void selectExecutor(Long clientId, Long orderId, SelectExecutorRequest request) {
        Order order = getOrderAndValidateClient(orderId, clientId);

        if (order.getStatus() != OrderStatus.NEW) {
            throw new BadRequestException("Can only select executor for orders in NEW status");
        }

        OrderResponse response = orderResponseRepository.findById(request.getResponseId())
                .orElseThrow(() -> new ResourceNotFoundException("Response", "id", request.getResponseId()));

        if (!response.getOrder().getId().equals(orderId)) {
            throw new BadRequestException("Response does not belong to this order");
        }

        User executor = response.getExecutor();

        // Mark response as selected
        response.setIsSelected(true);
        orderResponseRepository.save(response);

        // Update order
        order.setExecutor(executor);
        order.setAgreedPrice(request.getAgreedPrice() != null ? request.getAgreedPrice() : response.getProposedPrice());
        order.setAgreedDeadline(request.getAgreedDeadline());
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setStartedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Get or create chat room
        ChatRoom chatRoom = chatRoomRepository.findByOrderIdAndExecutorId(orderId, executor.getId())
                .orElseGet(() -> {
                    try {
                        ChatRoom newRoom = ChatRoom.builder()
                                .order(order)
                                .client(order.getClient())
                                .executor(executor)
                                .build();
                        return chatRoomRepository.save(newRoom);
                    } catch (DataIntegrityViolationException e) {
                        return chatRoomRepository.findByOrderId(orderId)
                                .orElseThrow(() -> new ResourceNotFoundException("ChatRoom", "orderId", orderId));
                    }
                });

        // Send system message to notify executor
        String systemMessageText = String.format(
                "Поздравляем! Вы выбраны исполнителем для заказа \"%s\". Можете приступать к работе.",
                order.getTitle()
        );

        Message systemMessage = Message.builder()
                .chatRoom(chatRoom)
                .sender(order.getClient())
                .content(systemMessageText)
                .isRead(false)
                .build();
        systemMessage = messageRepository.save(systemMessage);

        // Update chat room last message time
        chatRoom.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        // Send WebSocket notification to executor
        WsMessage wsMessage = WsMessage.builder()
                .id(systemMessage.getId())
                .chatRoomId(chatRoom.getId())
                .senderId(order.getClient().getId())
                .senderName(order.getClient().getFullName())
                .senderAvatarUrl(order.getClient().getAvatarUrl())
                .content(systemMessageText)
                .createdAt(systemMessage.getCreatedAt())
                .type(WsMessage.MessageType.SYSTEM)
                .build();

        messagingTemplate.convertAndSendToUser(
                executor.getEmail(),
                "/queue/messages",
                wsMessage
        );

        // Update executor stats
        ExecutorProfile profile = executorProfileRepository.findById(executor.getId()).orElse(null);
        if (profile != null) {
            profile.setTotalOrders(profile.getTotalOrders() + 1);
            profile.setLastActiveAt(LocalDateTime.now());
            executorProfileRepository.save(profile);
        }

        // Send email notification
        emailService.sendExecutorSelected(executor, order);

        // In-app notification to executor
        inAppNotificationService.send(
                executor,
                NotificationType.EXECUTOR_SELECTED,
                "Вы выбраны исполнителем",
                "Вы выбраны исполнителем для заказа \"" + order.getTitle() + "\". Можете приступать к работе.",
                order,
                "/orders/" + order.getId()
        );
    }

    @Override
    @Transactional
    public void startWork(Long executorId, Long orderId) {
        Order order = getOrderAndValidateExecutor(orderId, executorId);

        if (order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new BadRequestException("Order is not in IN_PROGRESS status");
        }

        // Work already started when executor was selected
    }

    @Override
    @Transactional
    public void submitForReview(Long executorId, Long orderId) {
        Order order = getOrderAndValidateExecutor(orderId, executorId);

        if (order.getStatus() != OrderStatus.IN_PROGRESS && order.getStatus() != OrderStatus.REVISION) {
            throw new BadRequestException("Order must be in IN_PROGRESS or REVISION status");
        }

        order.setStatus(OrderStatus.ON_REVIEW);
        orderRepository.save(order);

        // Send email notification to client
        emailService.sendWorkSubmittedForReview(order.getClient(), order);

        // Send chat message to client
        try {
            var chatRoom = chatService.getOrCreateChatRoom(orderId, executorId, order.getClient().getId());
            ChatMessageRequest messageRequest = new ChatMessageRequest();
            messageRequest.setContent("✅ Работа выполнена и отправлена на проверку. Пожалуйста, проверьте результат.");
            chatService.sendMessageWs(chatRoom.getId(), executorId, messageRequest);
        } catch (Exception e) {
            // Log but don't fail the main operation if chat message fails
        }
    }

    @Override
    @Transactional
    public void approveWork(Long clientId, Long orderId) {
        Order order = getOrderAndValidateClient(orderId, clientId);

        if (order.getStatus() != OrderStatus.ON_REVIEW) {
            throw new BadRequestException("Order must be in ON_REVIEW status");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Update executor stats
        if (order.getExecutor() != null) {
            ExecutorProfile profile = executorProfileRepository.findById(order.getExecutor().getId()).orElse(null);
            if (profile != null) {
                profile.setCompletedOrders(profile.getCompletedOrders() + 1);

                // Update average completion days
                if (order.getStartedAt() != null) {
                    long days = Math.max(1, ChronoUnit.DAYS.between(order.getStartedAt(), order.getCompletedAt()));
                    int completed = profile.getCompletedOrders();
                    double oldAvg = profile.getAvgCompletionDays() != null ? profile.getAvgCompletionDays() : 0.0;
                    double newAvg = ((oldAvg * (completed - 1)) + days) / completed;
                    profile.setAvgCompletionDays(newAvg);
                }

                executorProfileRepository.save(profile);
            }

            // Send email notification to executor
            emailService.sendWorkApproved(order.getExecutor(), order);

            // In-app notification to executor
            inAppNotificationService.send(
                    order.getExecutor(),
                    NotificationType.WORK_APPROVED,
                    "Работа принята",
                    "Ваша работа по заказу \"" + order.getTitle() + "\" принята заказчиком.",
                    order,
                    "/orders/" + order.getId()
            );
        }
    }

    @Override
    @Transactional
    public void requestRevision(Long clientId, Long orderId, String reason) {
        Order order = getOrderAndValidateClient(orderId, clientId);

        if (order.getStatus() != OrderStatus.ON_REVIEW) {
            throw new BadRequestException("Order must be in ON_REVIEW status");
        }

        order.setStatus(OrderStatus.REVISION);
        orderRepository.save(order);

        // Send notification to executor
        if (order.getExecutor() != null) {
            ChatRoom chatRoom = chatRoomRepository.findByOrderIdAndExecutorId(orderId, order.getExecutor().getId())
                    .orElse(null);

            if (chatRoom != null) {
                String messageText = "Заказчик запросил доработку по заказу \"" + order.getTitle() + "\".";
                if (reason != null && !reason.isBlank()) {
                    messageText += "\n\nПричина: " + reason;
                }

                Message systemMessage = Message.builder()
                        .chatRoom(chatRoom)
                        .sender(order.getClient())
                        .content(messageText)
                        .isRead(false)
                        .build();
                systemMessage = messageRepository.save(systemMessage);

                chatRoom.setLastMessageAt(LocalDateTime.now());
                chatRoomRepository.save(chatRoom);

                WsMessage wsMessage = WsMessage.builder()
                        .id(systemMessage.getId())
                        .chatRoomId(chatRoom.getId())
                        .senderId(order.getClient().getId())
                        .senderName(order.getClient().getFullName())
                        .senderAvatarUrl(order.getClient().getAvatarUrl())
                        .content(messageText)
                        .createdAt(systemMessage.getCreatedAt())
                        .type(WsMessage.MessageType.SYSTEM)
                        .build();

                messagingTemplate.convertAndSendToUser(
                        order.getExecutor().getEmail(),
                        "/queue/messages",
                        wsMessage
                );
            }

            // Send email notification
            emailService.sendRevisionRequested(order.getExecutor(), order, reason);

            // In-app notification to executor
            String notifMessage = "Заказчик запросил доработку по заказу \"" + order.getTitle() + "\".";
            if (reason != null && !reason.isBlank()) {
                notifMessage += " Причина: " + reason;
            }
            inAppNotificationService.send(
                    order.getExecutor(),
                    NotificationType.REVISION_REQUESTED,
                    "Запрошена доработка",
                    notifMessage,
                    order,
                    "/orders/" + order.getId()
            );
        }
    }

    @Override
    @Transactional
    public void openDispute(Long userId, Long orderId, String reason) {
        OpenDisputeRequest request = new OpenDisputeRequest(reason != null ? reason : "");
        disputeService.openDispute(userId, orderId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderListResponse> getMyOrdersAsClient(Long clientId, Pageable pageable) {
        Page<Order> page = orderRepository.findByClientIdOrderByCreatedAtDesc(clientId, pageable);

        List<OrderListResponse> content = page.getContent().stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderListResponse> getMyOrdersAsExecutor(Long executorId, Pageable pageable) {
        // Get all orders where user has responded (including pending and selected)
        Page<OrderResponse> responsePage = orderResponseRepository.findByExecutorIdOrderByCreatedAtDesc(executorId, pageable);

        List<OrderListResponse> content = responsePage.getContent().stream()
                .map(response -> mapToListResponseWithExecutorStatus(response.getOrder(), response.getIsSelected()))
                .collect(Collectors.toList());

        return PageResponse.of(responsePage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrderResponses(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Only client can see all responses
        if (!order.getClient().getId().equals(userId)) {
            throw new ForbiddenException("Only order owner can view responses");
        }

        return orderResponseRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(this::mapResponseToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponseDto createResponse(Long executorId, Long orderId, OrderResponseRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getStatus() != OrderStatus.NEW) {
            throw new BadRequestException("Can only respond to orders in NEW status");
        }

        if (order.getClient().getId().equals(executorId)) {
            throw new BadRequestException("Cannot respond to your own order");
        }

        if (orderResponseRepository.existsByOrderIdAndExecutorId(orderId, executorId)) {
            throw new BadRequestException("You have already responded to this order");
        }

        if (!executorProfileRepository.existsById(executorId)) {
            throw new BadRequestException("You need to create an executor profile first");
        }

        User executor = userRepository.findById(executorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", executorId));

        // Check verification and subscription access
        if (!subscriptionService.canAccessOrders(executor)) {
            if (!executor.getExecutorVerified()) {
                throw new ForbiddenException("Требуется верификация для отклика на заказы");
            } else {
                throw new ForbiddenException("Требуется активная подписка для отклика на заказы");
            }
        }

        OrderResponse response = OrderResponse.builder()
                .order(order)
                .executor(executor)
                .coverLetter(request.getCoverLetter())
                .proposedPrice(request.getProposedPrice())
                .proposedDays(request.getProposedDays())
                .isSelected(false)
                .build();

        response = orderResponseRepository.save(response);

        // Update response count
        orderRepository.incrementResponseCount(orderId);

        // Send email notification to client
        emailService.sendNewOrderResponse(order.getClient(), order, executor);

        // In-app notification to client
        inAppNotificationService.send(
                order.getClient(),
                NotificationType.NEW_RESPONSE,
                "Новый отклик на заказ",
                executor.getFullName() + " откликнулся на ваш заказ \"" + order.getTitle() + "\".",
                order,
                "/orders/" + order.getId()
        );

        return mapResponseToDto(response);
    }

    @Override
    @Transactional
    public OrderResponseDto updateResponse(Long executorId, Long responseId, OrderResponseRequest request) {
        OrderResponse response = orderResponseRepository.findById(responseId)
                .orElseThrow(() -> new ResourceNotFoundException("Response", "id", responseId));

        if (!response.getExecutor().getId().equals(executorId)) {
            throw new ForbiddenException("You can only update your own responses");
        }

        if (response.getIsSelected()) {
            throw new BadRequestException("Cannot update selected response");
        }

        if (request.getCoverLetter() != null) response.setCoverLetter(request.getCoverLetter());
        if (request.getProposedPrice() != null) response.setProposedPrice(request.getProposedPrice());
        if (request.getProposedDays() != null) response.setProposedDays(request.getProposedDays());

        response = orderResponseRepository.save(response);
        return mapResponseToDto(response);
    }

    @Override
    @Transactional
    public void deleteResponse(Long executorId, Long responseId) {
        OrderResponse response = orderResponseRepository.findById(responseId)
                .orElseThrow(() -> new ResourceNotFoundException("Response", "id", responseId));

        if (!response.getExecutor().getId().equals(executorId)) {
            throw new ForbiddenException("You can only delete your own responses");
        }

        if (response.getIsSelected()) {
            throw new BadRequestException("Cannot delete selected response");
        }

        orderResponseRepository.delete(response);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponseDto> getMyResponses(Long executorId, Pageable pageable) {
        Page<OrderResponse> page = orderResponseRepository.findByExecutorIdOrderByCreatedAtDesc(executorId, pageable);

        List<OrderResponseDto> content = page.getContent().stream()
                .map(this::mapResponseToDto)
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
    }

    // Helper methods

    private Order getOrderAndValidateClient(Long orderId, Long clientId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("You are not the owner of this order");
        }

        return order;
    }

    private Order getOrderAndValidateExecutor(Long orderId, Long executorId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getExecutor() == null || !order.getExecutor().getId().equals(executorId)) {
            throw new ForbiddenException("You are not the executor of this order");
        }

        return order;
    }

    private OrderListResponse mapToListResponse(Order order) {
        return OrderListResponse.builder()
                .id(order.getId())
                .title(order.getTitle())
                .description(order.getDescription())
                .categoryId(order.getCategory().getId())
                .categoryName(order.getCategory().getName())
                .clientId(order.getClient().getId())
                .clientName(order.getClient().getFullName())
                .budgetMin(order.getBudgetMin())
                .budgetMax(order.getBudgetMax())
                .deadline(order.getDeadline())
                .location(order.getLocation())
                .status(order.getStatus())
                .responseCount(order.getResponseCount())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderListResponse mapToListResponseWithExecutorStatus(Order order, Boolean isExecutorSelected) {
        return OrderListResponse.builder()
                .id(order.getId())
                .title(order.getTitle())
                .description(order.getDescription())
                .categoryId(order.getCategory().getId())
                .categoryName(order.getCategory().getName())
                .clientId(order.getClient().getId())
                .clientName(order.getClient().getFullName())
                .budgetMin(order.getBudgetMin())
                .budgetMax(order.getBudgetMax())
                .deadline(order.getDeadline())
                .location(order.getLocation())
                .status(order.getStatus())
                .responseCount(order.getResponseCount())
                .createdAt(order.getCreatedAt())
                .isExecutorSelected(isExecutorSelected)
                .build();
    }

    private OrderListResponse mapToListResponseWithUserContext(Order order, Long userId) {
        boolean hasResponded = userId != null && orderResponseRepository.existsByOrderIdAndExecutorId(order.getId(), userId);
        return OrderListResponse.builder()
                .id(order.getId())
                .title(order.getTitle())
                .description(order.getDescription())
                .categoryId(order.getCategory().getId())
                .categoryName(order.getCategory().getName())
                .clientId(order.getClient().getId())
                .clientName(order.getClient().getFullName())
                .budgetMin(order.getBudgetMin())
                .budgetMax(order.getBudgetMax())
                .deadline(order.getDeadline())
                .location(order.getLocation())
                .status(order.getStatus())
                .responseCount(order.getResponseCount())
                .createdAt(order.getCreatedAt())
                .hasResponded(hasResponded)
                .build();
    }

    private OrderDetailResponse mapToDetailResponse(Order order, Long userId) {
        boolean isOwner = userId != null && order.getClient().getId().equals(userId);
        boolean isExecutor = order.getExecutor() != null && userId != null && order.getExecutor().getId().equals(userId);
        boolean hasResponded = userId != null && orderResponseRepository.existsByOrderIdAndExecutorId(order.getId(), userId);

        // Check if user can see full description
        // Full description visible for: owner, executor, or verified users with active subscription (if required)
        boolean isVerified = userId != null && executorVerificationService.isVerified(userId);
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        boolean hasSubscriptionAccess = user != null && subscriptionService.canAccessOrders(user);
        boolean canSeeFullDescription = isOwner || isExecutor || (isVerified && hasSubscriptionAccess);

        String description = order.getDescription();
        boolean descriptionTruncated = false;
        boolean requiresVerification = false;
        boolean requiresSubscription = false;

        if (!canSeeFullDescription && description != null && description.length() > 200) {
            description = description.substring(0, 200) + "...";
            descriptionTruncated = true;
            if (!isVerified) {
                requiresVerification = true;
            } else if (!hasSubscriptionAccess) {
                requiresSubscription = true;
            }
        }

        return OrderDetailResponse.builder()
                .id(order.getId())
                .title(order.getTitle())
                .description(description)
                .categoryId(order.getCategory().getId())
                .categoryName(order.getCategory().getName())
                .clientId(order.getClient().getId())
                .clientName(order.getClient().getFullName())
                .clientAvatarUrl(order.getClient().getAvatarUrl())
                .executorId(order.getExecutor() != null ? order.getExecutor().getId() : null)
                .executorName(order.getExecutor() != null ? order.getExecutor().getFullName() : null)
                .executorAvatarUrl(order.getExecutor() != null ? order.getExecutor().getAvatarUrl() : null)
                .budgetMin(order.getBudgetMin())
                .budgetMax(order.getBudgetMax())
                .agreedPrice(order.getAgreedPrice())
                .location(order.getLocation())
                .deadline(order.getDeadline())
                .agreedDeadline(order.getAgreedDeadline())
                .status(order.getStatus())
                .isPublic(order.getIsPublic())
                .viewCount(order.getViewCount())
                .responseCount(order.getResponseCount())
                .attachments(order.getAttachments())
                .createdAt(order.getCreatedAt())
                .startedAt(order.getStartedAt())
                .completedAt(order.getCompletedAt())
                .isOwner(isOwner)
                .isExecutor(isExecutor)
                .hasResponded(hasResponded)
                .descriptionTruncated(descriptionTruncated)
                .requiresVerification(requiresVerification)
                .requiresSubscription(requiresSubscription)
                .build();
    }

    private OrderResponseDto mapResponseToDto(OrderResponse response) {
        User executor = response.getExecutor();
        ExecutorProfile profile = executorProfileRepository.findById(executor.getId()).orElse(null);

        return OrderResponseDto.builder()
                .id(response.getId())
                .orderId(response.getOrder().getId())
                .orderTitle(response.getOrder().getTitle())
                .orderStatus(response.getOrder().getStatus())
                .executorId(executor.getId())
                .executorName(executor.getFullName())
                .executorAvatarUrl(executor.getAvatarUrl())
                .executorSpecialization(profile != null ? profile.getSpecialization() : null)
                .executorRating(profile != null ? profile.getRating() : null)
                .executorCompletedOrders(profile != null ? profile.getCompletedOrders() : null)
                .coverLetter(response.getCoverLetter())
                .proposedPrice(response.getProposedPrice())
                .proposedDays(response.getProposedDays())
                .isSelected(response.getIsSelected())
                .createdAt(response.getCreatedAt())
                .build();
    }
}
