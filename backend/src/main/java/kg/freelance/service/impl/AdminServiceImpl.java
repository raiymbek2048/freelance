package kg.freelance.service.impl;

import kg.freelance.dto.request.CategoryCreateRequest;
import kg.freelance.dto.response.*;
import kg.freelance.entity.*;
import kg.freelance.entity.enums.OrderStatus;
import kg.freelance.entity.enums.SubscriptionStatus;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.*;
import kg.freelance.dto.request.ResolveDisputeRequest;
import kg.freelance.repository.DisputeRepository;
import kg.freelance.service.AdminService;
import kg.freelance.service.DisputeService;
import kg.freelance.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final ExecutorProfileRepository executorProfileRepository;
    private final ReviewService reviewService;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final DisputeRepository disputeRepository;
    private final DisputeService disputeService;
    private final SubscriptionSettingsRepository subscriptionSettingsRepository;
    private final OrderResponseRepository orderResponseRepository;

    // ==================== USERS ====================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getAllUsers(String search, Boolean active, UserRole role, Pageable pageable) {
        // Simple implementation - in production use Specification or QueryDSL
        Page<User> page = userRepository.findAll(pageable);

        List<AdminUserResponse> content = page.getContent().stream()
                .filter(u -> search == null || u.getFullName().toLowerCase().contains(search.toLowerCase())
                        || u.getEmail().toLowerCase().contains(search.toLowerCase()))
                .filter(u -> active == null || u.getActive().equals(active))
                .filter(u -> role == null || u.getRole().equals(role))
                .map(this::mapToAdminUserResponse)
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToAdminUserResponse(user);
    }

    @Override
    @Transactional
    public void blockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void unblockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changeUserRole(Long userId, UserRole role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setRole(role);
        userRepository.save(user);
    }

    // ==================== ORDERS ====================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderResponse> getAllOrders(OrderStatus status, Long categoryId, Pageable pageable) {
        Page<Order> page = orderRepository.findAll(pageable);

        List<AdminOrderResponse> content = page.getContent().stream()
                .filter(o -> status == null || o.getStatus().equals(status))
                .filter(o -> categoryId == null || o.getCategory().getId().equals(categoryId))
                .map(this::mapToAdminOrderResponse)
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderResponse> getDisputedOrders(Pageable pageable) {
        Page<Order> page = orderRepository.findAll(pageable);

        List<AdminOrderResponse> content = page.getContent().stream()
                .filter(o -> o.getStatus() == OrderStatus.DISPUTED)
                .map(this::mapToAdminOrderResponse)
                .collect(Collectors.toList());

        Page<AdminOrderResponse> filteredPage = new PageImpl<>(content, pageable, content.size());
        return PageResponse.of(filteredPage);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return mapToAdminOrderResponse(order);
    }

    @Override
    @Transactional
    public void resolveDispute(Long orderId, boolean favorClient, String resolution) {
        kg.freelance.entity.Dispute dispute = disputeRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BadRequestException("No dispute found for order " + orderId));

        // Find an admin user to attribute the resolution (use first admin)
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        Long adminId = admins.isEmpty() ? null : admins.get(0).getId();

        if (adminId != null) {
            ResolveDisputeRequest request = new ResolveDisputeRequest(favorClient, resolution, null);
            disputeService.resolveDispute(dispute.getId(), adminId, request);
        } else {
            // Fallback: resolve directly if no admin found
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

            if (favorClient) {
                order.setStatus(OrderStatus.CANCELLED);
            } else {
                order.setStatus(OrderStatus.COMPLETED);
                order.setCompletedAt(LocalDateTime.now());
            }
            orderRepository.save(order);
        }
    }

    @Override
    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        orderRepository.delete(order);
    }

    // ==================== CATEGORIES ====================

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("Category with this slug already exists");
        }

        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with this name already exists");
        }

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getParentId()));
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .parent(parent)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .active(true)
                .build();

        category = categoryRepository.save(category);

        return mapToCategoryResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryCreateRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByName(request.getName())) {
                throw new BadRequestException("Category with this name already exists");
            }
            category.setName(request.getName());
        }

        if (request.getSlug() != null && !request.getSlug().equals(category.getSlug())) {
            if (categoryRepository.existsBySlug(request.getSlug())) {
                throw new BadRequestException("Category with this slug already exists");
            }
            category.setSlug(request.getSlug());
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getIconUrl() != null) {
            category.setIconUrl(request.getIconUrl());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getParentId()));
            category.setParent(parent);
        }

        category = categoryRepository.save(category);
        return mapToCategoryResponse(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        // Soft delete - just deactivate
        category.setActive(false);
        categoryRepository.save(category);
    }

    // ==================== REVIEWS ====================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminReviewResponse> getPendingReviews(Pageable pageable) {
        Page<Review> page = reviewRepository.findByIsModeratedFalseOrderByCreatedAtAsc(pageable);

        List<AdminReviewResponse> content = page.getContent().stream()
                .map(this::mapToAdminReviewResponse)
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminReviewResponse> getAllReviews(Boolean moderated, Pageable pageable) {
        Page<Review> page = reviewRepository.findAll(pageable);

        List<AdminReviewResponse> content = page.getContent().stream()
                .filter(r -> moderated == null || r.getIsModerated().equals(moderated))
                .map(this::mapToAdminReviewResponse)
                .collect(Collectors.toList());

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional
    public void approveReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        review.setIsModerated(true);
        review.setIsVisible(true);
        reviewRepository.save(review);

        // Recalculate rating
        reviewService.recalculateExecutorRating(review.getExecutor().getId());
    }

    @Override
    @Transactional
    public void rejectReview(Long reviewId, String reason) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        review.setIsModerated(true);
        review.setIsVisible(false);
        review.setModeratorComment(reason);
        reviewRepository.save(review);

        // Recalculate rating
        reviewService.recalculateExecutorRating(review.getExecutor().getId());
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        Long executorId = review.getExecutor().getId();
        reviewRepository.delete(review);

        // Recalculate rating
        reviewService.recalculateExecutorRating(executorId);
    }

    // ==================== STATS ====================

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponse getOverviewStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.with(LocalTime.MIN);
        LocalDateTime weekStart = now.minusDays(7);
        LocalDateTime monthStart = now.minusDays(30);

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActiveTrue();
        long executors = executorProfileRepository.count();
        long totalOrders = orderRepository.count();
        long totalReviews = reviewRepository.count();

        long newUsersToday = userRepository.countByCreatedAtAfter(todayStart);
        long newUsersThisWeek = userRepository.countByCreatedAtAfter(weekStart);
        long newUsersThisMonth = userRepository.countByCreatedAtAfter(monthStart);

        long newOrders = orderRepository.countByStatus(OrderStatus.NEW);
        long inProgressOrders = orderRepository.countByStatus(OrderStatus.IN_PROGRESS)
                + orderRepository.countByStatus(OrderStatus.REVISION);
        long completedOrders = orderRepository.countByStatus(OrderStatus.COMPLETED);
        long disputedOrders = orderRepository.countByStatus(OrderStatus.DISPUTED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);

        BigDecimal totalOrdersValue = orderRepository.sumAgreedPrice();
        long ordersWithPrice = orderRepository.countWithAgreedPrice();
        BigDecimal averageOrderValue = ordersWithPrice > 0
                ? totalOrdersValue.divide(BigDecimal.valueOf(ordersWithPrice), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long pendingModeration = reviewRepository.countByIsModeratedFalse();
        Double avgRating = reviewRepository.calculateOverallAverageRating();
        if (avgRating == null) avgRating = 0.0;

        // Top categories by order count (efficient single query)
        List<Object[]> categoryOrderCounts = orderRepository.countOrdersByCategory();
        List<AdminStatsResponse.CategoryStats> topCategories = categoryOrderCounts.stream()
                .limit(5)
                .map(row -> AdminStatsResponse.CategoryStats.builder()
                        .categoryId((Long) row[0])
                        .categoryName((String) row[1])
                        .orderCount((Long) row[2])
                        .executorCount(0L)
                        .build())
                .collect(Collectors.toList());

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .executors(executors)
                .newUsersToday(newUsersToday)
                .newUsersThisWeek(newUsersThisWeek)
                .newUsersThisMonth(newUsersThisMonth)
                .totalOrders(totalOrders)
                .newOrders(newOrders)
                .inProgressOrders(inProgressOrders)
                .completedOrders(completedOrders)
                .disputedOrders(disputedOrders)
                .cancelledOrders(cancelledOrders)
                .totalOrdersValue(totalOrdersValue)
                .averageOrderValue(averageOrderValue)
                .totalReviews(totalReviews)
                .pendingModeration(pendingModeration)
                .averageRating(avgRating)
                .totalCategories(categoryRepository.count())
                .topCategories(topCategories)
                .build();
    }

    // ==================== MAPPERS ====================

    private AdminUserResponse mapToAdminUserResponse(User user) {
        ExecutorProfile profile = user.getExecutorProfile();

        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .profileVisibility(user.getProfileVisibility())
                .hideFromExecutorList(user.getHideFromExecutorList())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .role(user.getRole())
                .active(user.getActive())
                .executorVerified(user.getExecutorVerified())
                .createdAt(user.getCreatedAt())
                .hasExecutorProfile(profile != null)
                .totalOrders(profile != null ? profile.getTotalOrders() : null)
                .completedOrders(profile != null ? profile.getCompletedOrders() : null)
                .rating(profile != null ? profile.getRating() : null)
                .reviewCount(profile != null ? profile.getReviewCount() : null)
                .ordersAsClient(user.getOrdersAsClient().size())
                .ordersAsExecutor(user.getOrdersAsExecutor().size())
                .build();
    }

    private AdminOrderResponse mapToAdminOrderResponse(Order order) {
        Review review = order.getReview();

        return AdminOrderResponse.builder()
                .id(order.getId())
                .title(order.getTitle())
                .description(order.getDescription())
                .categoryId(order.getCategory().getId())
                .categoryName(order.getCategory().getName())
                .clientId(order.getClient().getId())
                .clientName(order.getClient().getFullName())
                .clientEmail(order.getClient().getEmail())
                .executorId(order.getExecutor() != null ? order.getExecutor().getId() : null)
                .executorName(order.getExecutor() != null ? order.getExecutor().getFullName() : null)
                .executorEmail(order.getExecutor() != null ? order.getExecutor().getEmail() : null)
                .budgetMin(order.getBudgetMin())
                .budgetMax(order.getBudgetMax())
                .agreedPrice(order.getAgreedPrice())
                .deadline(order.getDeadline())
                .agreedDeadline(order.getAgreedDeadline())
                .createdAt(order.getCreatedAt())
                .startedAt(order.getStartedAt())
                .completedAt(order.getCompletedAt())
                .status(order.getStatus())
                .isPublic(order.getIsPublic())
                .responseCount(order.getResponseCount())
                .viewCount(order.getViewCount())
                .hasReview(review != null)
                .reviewRating(review != null ? review.getRating() : null)
                .build();
    }

    private AdminReviewResponse mapToAdminReviewResponse(Review review) {
        return AdminReviewResponse.builder()
                .id(review.getId())
                .orderId(review.getOrder().getId())
                .orderTitle(review.getOrder().getTitle())
                .clientId(review.getClient().getId())
                .clientName(review.getClient().getFullName())
                .clientEmail(review.getClient().getEmail())
                .executorId(review.getExecutor().getId())
                .executorName(review.getExecutor().getFullName())
                .executorEmail(review.getExecutor().getEmail())
                .rating(review.getRating())
                .comment(review.getComment())
                .isModerated(review.getIsModerated())
                .isVisible(review.getIsVisible())
                .moderatorComment(review.getModeratorComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private CategoryResponse mapToCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .sortOrder(category.getSortOrder())
                .build();
    }

    // ==================== ANALYTICS ====================

    @Override
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        BigDecimal subscriptionPrice = subscriptionSettingsRepository.getSettings().getPrice();

        // Daily stats (last 30 days)
        List<AnalyticsResponse.DailyStats> dailyStats = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            long newUsers = userRepository.countByCreatedAtBetween(dayStart, dayEnd);
            long newOrders = orderRepository.countByCreatedAtBetween(dayStart, dayEnd);
            long completedOrders = orderRepository.countCompletedBetween(dayStart, dayEnd);
            long newActiveSubscriptions = userSubscriptionRepository.countByStatusAndCreatedAtBetween(
                    SubscriptionStatus.ACTIVE, dayStart, dayEnd);
            BigDecimal revenue = subscriptionPrice.multiply(BigDecimal.valueOf(newActiveSubscriptions));

            dailyStats.add(AnalyticsResponse.DailyStats.builder()
                    .date(date)
                    .newUsers(newUsers)
                    .newOrders(newOrders)
                    .completedOrders(completedOrders)
                    .revenue(revenue)
                    .build());
        }

        // Weekly stats (last 12 weeks)
        List<AnalyticsResponse.WeeklyStats> weeklyStats = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            LocalDate weekStart = today.minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);
            LocalDateTime weekStartTime = weekStart.atStartOfDay();
            LocalDateTime weekEndTime = weekEnd.atTime(LocalTime.MAX);

            long newUsers = userRepository.countByCreatedAtBetween(weekStartTime, weekEndTime);
            long newOrders = orderRepository.countByCreatedAtBetween(weekStartTime, weekEndTime);
            long completedOrders = orderRepository.countCompletedBetween(weekStartTime, weekEndTime);
            long newActiveSubscriptions = userSubscriptionRepository.countByStatusAndCreatedAtBetween(
                    SubscriptionStatus.ACTIVE, weekStartTime, weekEndTime);
            BigDecimal revenue = subscriptionPrice.multiply(BigDecimal.valueOf(newActiveSubscriptions));

            weeklyStats.add(AnalyticsResponse.WeeklyStats.builder()
                    .weekStart(weekStart)
                    .newUsers(newUsers)
                    .newOrders(newOrders)
                    .completedOrders(completedOrders)
                    .revenue(revenue)
                    .build());
        }

        // Monthly stats (last 12 months)
        List<AnalyticsResponse.MonthlyStats> monthlyStats = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            LocalDateTime monthStartTime = monthStart.atStartOfDay();
            LocalDateTime monthEndTime = monthEnd.atTime(LocalTime.MAX);

            long newUsers = userRepository.countByCreatedAtBetween(monthStartTime, monthEndTime);
            long newOrders = orderRepository.countByCreatedAtBetween(monthStartTime, monthEndTime);
            long completedOrders = orderRepository.countCompletedBetween(monthStartTime, monthEndTime);
            long newActiveSubscriptions = userSubscriptionRepository.countByStatusAndCreatedAtBetween(
                    SubscriptionStatus.ACTIVE, monthStartTime, monthEndTime);
            BigDecimal revenue = subscriptionPrice.multiply(BigDecimal.valueOf(newActiveSubscriptions));

            String monthName = monthStart.getMonth().getDisplayName(TextStyle.SHORT, new Locale("ru"));

            monthlyStats.add(AnalyticsResponse.MonthlyStats.builder()
                    .year(monthStart.getYear())
                    .month(monthStart.getMonthValue())
                    .monthName(monthName)
                    .newUsers(newUsers)
                    .newOrders(newOrders)
                    .completedOrders(completedOrders)
                    .revenue(revenue)
                    .build());
        }

        // Subscription analytics
        long totalSubscriptions = userSubscriptionRepository.count();
        long activeSubscriptions = userSubscriptionRepository.countCurrentlyActive(now);
        long trialSubscriptions = userSubscriptionRepository.countCurrentlyTrial(now);
        long expiredSubscriptions = userSubscriptionRepository.countExpired(now);

        long totalPaidSubscriptions = userSubscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        BigDecimal totalRevenue = subscriptionPrice.multiply(BigDecimal.valueOf(totalPaidSubscriptions));

        LocalDateTime thisMonthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastMonthStart = today.minusMonths(1).withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastMonthEnd = thisMonthStart.minusSeconds(1);

        long subscriptionsThisMonth = userSubscriptionRepository.countByStatusAndCreatedAtBetween(
                SubscriptionStatus.ACTIVE, thisMonthStart, now);
        long subscriptionsLastMonth = userSubscriptionRepository.countByStatusAndCreatedAtBetween(
                SubscriptionStatus.ACTIVE, lastMonthStart, lastMonthEnd);

        BigDecimal revenueThisMonth = subscriptionPrice.multiply(BigDecimal.valueOf(subscriptionsThisMonth));
        BigDecimal revenueLastMonth = subscriptionPrice.multiply(BigDecimal.valueOf(subscriptionsLastMonth));

        // Subscription by period (last 30 days)
        List<AnalyticsResponse.SubscriptionByPeriod> subscriptionByPeriod = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            long newSubs = userSubscriptionRepository.countByStatusAndCreatedAtBetween(
                    SubscriptionStatus.ACTIVE, dayStart, dayEnd);

            subscriptionByPeriod.add(AnalyticsResponse.SubscriptionByPeriod.builder()
                    .date(date)
                    .newSubscriptions(newSubs)
                    .revenue(subscriptionPrice.multiply(BigDecimal.valueOf(newSubs)))
                    .build());
        }

        AnalyticsResponse.SubscriptionAnalytics subscriptionAnalytics = AnalyticsResponse.SubscriptionAnalytics.builder()
                .totalSubscriptions(totalSubscriptions)
                .activeSubscriptions(activeSubscriptions)
                .trialSubscriptions(trialSubscriptions)
                .expiredSubscriptions(expiredSubscriptions)
                .totalRevenue(totalRevenue)
                .revenueThisMonth(revenueThisMonth)
                .revenueLastMonth(revenueLastMonth)
                .byPeriod(subscriptionByPeriod)
                .build();

        // Conversion stats
        long totalUsersCount = userRepository.count();
        long executorsCount = executorProfileRepository.count();
        long verifiedExecutors = userRepository.countVerifiedExecutors();
        long totalOrdersCount = orderRepository.count();
        long completedOrdersCount = orderRepository.countByStatus(OrderStatus.COMPLETED);
        long totalResponsesCount = orderResponseRepository.count();
        long selectedResponsesCount = orderResponseRepository.countSelected();

        Double registrationToExecutorRate = totalUsersCount > 0
                ? (executorsCount * 100.0 / totalUsersCount)
                : 0.0;
        Double executorToVerifiedRate = executorsCount > 0
                ? (verifiedExecutors * 100.0 / executorsCount)
                : 0.0;
        Double orderCompletionRate = totalOrdersCount > 0
                ? (completedOrdersCount * 100.0 / totalOrdersCount)
                : 0.0;
        Double responseToSelectionRate = totalResponsesCount > 0
                ? (selectedResponsesCount * 100.0 / totalResponsesCount)
                : 0.0;

        AnalyticsResponse.ConversionStats conversionStats = AnalyticsResponse.ConversionStats.builder()
                .registrationToExecutorRate(round(registrationToExecutorRate))
                .executorToVerifiedRate(round(executorToVerifiedRate))
                .orderCompletionRate(round(orderCompletionRate))
                .responseToSelectionRate(round(responseToSelectionRate))
                .build();

        return AnalyticsResponse.builder()
                .dailyStats(dailyStats)
                .weeklyStats(weeklyStats)
                .monthlyStats(monthlyStats)
                .subscriptions(subscriptionAnalytics)
                .conversions(conversionStats)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAnalyticsCsv() {
        AnalyticsResponse analytics = getAnalytics();
        AdminStatsResponse overview = getOverviewStats();

        StringBuilder csv = new StringBuilder();
        csv.append("Аналитика платформы FreelanceKG\n\n");

        // Overview
        csv.append("ОБЗОР\n");
        csv.append("Показатель,Значение\n");
        csv.append("Всего пользователей,").append(overview.getTotalUsers()).append("\n");
        csv.append("Активных пользователей,").append(overview.getActiveUsers()).append("\n");
        csv.append("Исполнителей,").append(overview.getExecutors()).append("\n");
        csv.append("Всего заказов,").append(overview.getTotalOrders()).append("\n");
        csv.append("Завершённых заказов,").append(overview.getCompletedOrders()).append("\n");
        csv.append("Общая стоимость заказов,").append(overview.getTotalOrdersValue()).append("\n");
        csv.append("Средняя стоимость заказа,").append(overview.getAverageOrderValue()).append("\n");
        csv.append("\n");

        // Daily stats
        csv.append("ЕЖЕДНЕВНАЯ СТАТИСТИКА (последние 30 дней)\n");
        csv.append("Дата,Новые пользователи,Новые заказы,Завершённые заказы,Доход\n");
        for (AnalyticsResponse.DailyStats ds : analytics.getDailyStats()) {
            csv.append(ds.getDate()).append(",")
                    .append(ds.getNewUsers()).append(",")
                    .append(ds.getNewOrders()).append(",")
                    .append(ds.getCompletedOrders()).append(",")
                    .append(ds.getRevenue()).append("\n");
        }
        csv.append("\n");

        // Weekly stats
        csv.append("ЕЖЕНЕДЕЛЬНАЯ СТАТИСТИКА (последние 12 недель)\n");
        csv.append("Начало недели,Новые пользователи,Новые заказы,Завершённые заказы,Доход\n");
        for (AnalyticsResponse.WeeklyStats ws : analytics.getWeeklyStats()) {
            csv.append(ws.getWeekStart()).append(",")
                    .append(ws.getNewUsers()).append(",")
                    .append(ws.getNewOrders()).append(",")
                    .append(ws.getCompletedOrders()).append(",")
                    .append(ws.getRevenue()).append("\n");
        }
        csv.append("\n");

        // Monthly stats
        csv.append("ЕЖЕМЕСЯЧНАЯ СТАТИСТИКА (последние 12 месяцев)\n");
        csv.append("Месяц,Год,Новые пользователи,Новые заказы,Завершённые заказы,Доход\n");
        for (AnalyticsResponse.MonthlyStats ms : analytics.getMonthlyStats()) {
            csv.append(ms.getMonthName()).append(",")
                    .append(ms.getYear()).append(",")
                    .append(ms.getNewUsers()).append(",")
                    .append(ms.getNewOrders()).append(",")
                    .append(ms.getCompletedOrders()).append(",")
                    .append(ms.getRevenue()).append("\n");
        }
        csv.append("\n");

        // Subscriptions
        AnalyticsResponse.SubscriptionAnalytics subs = analytics.getSubscriptions();
        csv.append("ПОДПИСКИ\n");
        csv.append("Показатель,Значение\n");
        csv.append("Всего подписок,").append(subs.getTotalSubscriptions()).append("\n");
        csv.append("Активных,").append(subs.getActiveSubscriptions()).append("\n");
        csv.append("Триал,").append(subs.getTrialSubscriptions()).append("\n");
        csv.append("Истекших,").append(subs.getExpiredSubscriptions()).append("\n");
        csv.append("Общий доход,").append(subs.getTotalRevenue()).append("\n");
        csv.append("Доход за этот месяц,").append(subs.getRevenueThisMonth()).append("\n");
        csv.append("Доход за прошлый месяц,").append(subs.getRevenueLastMonth()).append("\n");
        csv.append("\n");

        // Conversions
        AnalyticsResponse.ConversionStats conv = analytics.getConversions();
        csv.append("КОНВЕРСИИ\n");
        csv.append("Показатель,Значение (%)\n");
        csv.append("Регистрация → Исполнитель,").append(conv.getRegistrationToExecutorRate()).append("\n");
        csv.append("Исполнитель → Верификация,").append(conv.getExecutorToVerifiedRate()).append("\n");
        csv.append("Завершение заказов,").append(conv.getOrderCompletionRate()).append("\n");
        csv.append("Отклик → Выбор,").append(conv.getResponseToSelectionRate()).append("\n");

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private Double round(Double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
