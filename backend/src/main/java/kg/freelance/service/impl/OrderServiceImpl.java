package kg.freelance.service.impl;

import kg.freelance.dto.request.*;
import kg.freelance.dto.response.*;
import kg.freelance.entity.*;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ForbiddenException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.*;
import kg.freelance.service.ExecutorVerificationService;
import kg.freelance.service.OrderService;
import kg.freelance.websocket.dto.WsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderListResponse> getPublicOrders(
            Long categoryId, BigDecimal budgetMin, BigDecimal budgetMax,
            String search, String location, Pageable pageable) {

        Page<Order> page = orderRepository.findPublicOrders(categoryId, budgetMin, budgetMax, search, location, pageable);

        List<OrderListResponse> content = page.getContent().stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional
    public OrderDetailResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Increment view count
        orderRepository.incrementViewCount(orderId);

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
                    ChatRoom newRoom = ChatRoom.builder()
                            .order(order)
                            .client(order.getClient())
                            .executor(executor)
                            .build();
                    return chatRoomRepository.save(newRoom);
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
                executorProfileRepository.save(profile);
            }
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
        }
    }

    @Override
    @Transactional
    public void openDispute(Long userId, Long orderId, String reason) {
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

        // Send notification to the other party
        if (order.getExecutor() != null) {
            User sender = isClient ? order.getClient() : order.getExecutor();
            User recipient = isClient ? order.getExecutor() : order.getClient();
            String initiatorRole = isClient ? "Заказчик" : "Исполнитель";

            ChatRoom chatRoom = chatRoomRepository.findByOrderIdAndExecutorId(orderId, order.getExecutor().getId())
                    .orElse(null);

            if (chatRoom != null) {
                String messageText = initiatorRole + " открыл спор по заказу \"" + order.getTitle() + "\".";
                if (reason != null && !reason.isBlank()) {
                    messageText += "\n\nПричина: " + reason;
                }
                messageText += "\n\nМодератор рассмотрит ситуацию и примет решение.";

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
        }

        // Notify all admins about the dispute
        notifyAdminsAboutDispute(order, reason, isClient ? "заказчиком" : "исполнителем");
    }

    private void notifyAdminsAboutDispute(Order order, String reason, String initiatedBy) {
        List<User> admins = userRepository.findByRole(kg.freelance.entity.enums.UserRole.ADMIN);

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
        Page<Order> page = orderRepository.findByExecutorIdOrderByCreatedAtDesc(executorId, pageable);

        List<OrderListResponse> content = page.getContent().stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
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

    private OrderDetailResponse mapToDetailResponse(Order order, Long userId) {
        boolean isOwner = userId != null && order.getClient().getId().equals(userId);
        boolean isExecutor = order.getExecutor() != null && userId != null && order.getExecutor().getId().equals(userId);
        boolean hasResponded = userId != null && orderResponseRepository.existsByOrderIdAndExecutorId(order.getId(), userId);

        // Check if user can see full description
        // Full description visible for: owner, executor, or verified users
        boolean isVerified = userId != null && executorVerificationService.isVerified(userId);
        boolean canSeeFullDescription = isOwner || isExecutor || isVerified;

        String description = order.getDescription();
        boolean descriptionTruncated = false;
        boolean requiresVerification = false;

        if (!canSeeFullDescription && description != null && description.length() > 200) {
            description = description.substring(0, 200) + "...";
            descriptionTruncated = true;
            requiresVerification = true;
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
                .build();
    }

    private OrderResponseDto mapResponseToDto(OrderResponse response) {
        User executor = response.getExecutor();
        ExecutorProfile profile = executorProfileRepository.findById(executor.getId()).orElse(null);

        return OrderResponseDto.builder()
                .id(response.getId())
                .orderId(response.getOrder().getId())
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
