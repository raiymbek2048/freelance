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
import kg.freelance.service.AdminService;
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getStatus() != OrderStatus.DISPUTED) {
            throw new BadRequestException("Order is not in dispute");
        }

        if (favorClient) {
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

        List<User> allUsers = userRepository.findAll();
        List<Order> allOrders = orderRepository.findAll();
        List<Review> allReviews = reviewRepository.findAll();

        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(User::getActive).count();
        long executors = executorProfileRepository.count();
        long totalOrders = allOrders.size();
        long totalReviews = allReviews.size();

        // New users by time period
        long newUsersToday = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(todayStart))
                .count();
        long newUsersThisWeek = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(weekStart))
                .count();
        long newUsersThisMonth = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(monthStart))
                .count();

        // Count orders by status
        long newOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.NEW).count();
        long inProgressOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS || o.getStatus() == OrderStatus.REVISION).count();
        long completedOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();
        long disputedOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DISPUTED).count();
        long cancelledOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();

        // Financial stats
        BigDecimal totalOrdersValue = allOrders.stream()
                .filter(o -> o.getAgreedPrice() != null)
                .map(Order::getAgreedPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long ordersWithPrice = allOrders.stream().filter(o -> o.getAgreedPrice() != null).count();
        BigDecimal averageOrderValue = ordersWithPrice > 0
                ? totalOrdersValue.divide(BigDecimal.valueOf(ordersWithPrice), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Pending moderation
        long pendingModeration = allReviews.stream()
                .filter(r -> !r.getIsModerated()).count();

        // Average rating
        Double avgRating = allReviews.stream()
                .filter(Review::getIsVisible)
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        // Top categories
        List<AdminStatsResponse.CategoryStats> topCategories = categoryRepository.findAll().stream()
                .limit(5)
                .map(cat -> {
                    long orderCount = allOrders.stream()
                            .filter(o -> o.getCategory().getId().equals(cat.getId()))
                            .count();
                    long executorCount = executorProfileRepository.findAll().stream()
                            .filter(ep -> ep.getCategories().stream()
                                    .anyMatch(c -> c.getId().equals(cat.getId())))
                            .count();
                    return AdminStatsResponse.CategoryStats.builder()
                            .categoryId(cat.getId())
                            .categoryName(cat.getName())
                            .orderCount(orderCount)
                            .executorCount(executorCount)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getOrderCount(), a.getOrderCount()))
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

        List<User> allUsers = userRepository.findAll();
        List<Order> allOrders = orderRepository.findAll();
        List<UserSubscription> allSubscriptions = userSubscriptionRepository.findAll();
        List<OrderResponse> allResponses = orderResponseRepository.findAll();

        BigDecimal subscriptionPrice = subscriptionSettingsRepository.getSettings().getPrice();

        // Daily stats (last 30 days)
        List<AnalyticsResponse.DailyStats> dailyStats = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            long newUsers = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null && isWithinPeriod(u.getCreatedAt(), dayStart, dayEnd))
                    .count();
            long newOrders = allOrders.stream()
                    .filter(o -> o.getCreatedAt() != null && isWithinPeriod(o.getCreatedAt(), dayStart, dayEnd))
                    .count();
            long completedOrders = allOrders.stream()
                    .filter(o -> o.getCompletedAt() != null && isWithinPeriod(o.getCompletedAt(), dayStart, dayEnd))
                    .count();
            long newActiveSubscriptions = allSubscriptions.stream()
                    .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE && isWithinPeriod(s.getCreatedAt(), dayStart, dayEnd))
                    .count();
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

            long newUsers = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null && isWithinPeriod(u.getCreatedAt(), weekStartTime, weekEndTime))
                    .count();
            long newOrders = allOrders.stream()
                    .filter(o -> o.getCreatedAt() != null && isWithinPeriod(o.getCreatedAt(), weekStartTime, weekEndTime))
                    .count();
            long completedOrders = allOrders.stream()
                    .filter(o -> o.getCompletedAt() != null && isWithinPeriod(o.getCompletedAt(), weekStartTime, weekEndTime))
                    .count();
            long newActiveSubscriptions = allSubscriptions.stream()
                    .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE && isWithinPeriod(s.getCreatedAt(), weekStartTime, weekEndTime))
                    .count();
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

            long newUsers = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null && isWithinPeriod(u.getCreatedAt(), monthStartTime, monthEndTime))
                    .count();
            long newOrders = allOrders.stream()
                    .filter(o -> o.getCreatedAt() != null && isWithinPeriod(o.getCreatedAt(), monthStartTime, monthEndTime))
                    .count();
            long completedOrders = allOrders.stream()
                    .filter(o -> o.getCompletedAt() != null && isWithinPeriod(o.getCompletedAt(), monthStartTime, monthEndTime))
                    .count();
            long newActiveSubscriptions = allSubscriptions.stream()
                    .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE && isWithinPeriod(s.getCreatedAt(), monthStartTime, monthEndTime))
                    .count();
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
        long totalSubscriptions = allSubscriptions.size();
        long activeSubscriptions = allSubscriptions.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE && s.getEndDate().isAfter(now))
                .count();
        long trialSubscriptions = allSubscriptions.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.TRIAL && s.getEndDate().isAfter(now))
                .count();
        long expiredSubscriptions = allSubscriptions.stream()
                .filter(s -> s.getEndDate().isBefore(now) || s.getStatus() == SubscriptionStatus.EXPIRED)
                .count();

        long totalPaidSubscriptions = allSubscriptions.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .count();
        BigDecimal totalRevenue = subscriptionPrice.multiply(BigDecimal.valueOf(totalPaidSubscriptions));

        LocalDateTime thisMonthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastMonthStart = today.minusMonths(1).withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastMonthEnd = thisMonthStart.minusSeconds(1);

        long subscriptionsThisMonth = allSubscriptions.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE && s.getCreatedAt().isAfter(thisMonthStart))
                .count();
        long subscriptionsLastMonth = allSubscriptions.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE && isWithinPeriod(s.getCreatedAt(), lastMonthStart, lastMonthEnd))
                .count();

        BigDecimal revenueThisMonth = subscriptionPrice.multiply(BigDecimal.valueOf(subscriptionsThisMonth));
        BigDecimal revenueLastMonth = subscriptionPrice.multiply(BigDecimal.valueOf(subscriptionsLastMonth));

        // Subscription by period (last 30 days)
        List<AnalyticsResponse.SubscriptionByPeriod> subscriptionByPeriod = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            long newSubs = allSubscriptions.stream()
                    .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE && isWithinPeriod(s.getCreatedAt(), dayStart, dayEnd))
                    .count();

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
        long totalUsersCount = allUsers.size();
        long executorsCount = executorProfileRepository.count();
        long verifiedExecutors = allUsers.stream()
                .filter(u -> Boolean.TRUE.equals(u.getExecutorVerified()))
                .count();
        long totalOrdersCount = allOrders.size();
        long completedOrdersCount = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .count();
        long totalResponsesCount = allResponses.size();
        long selectedResponsesCount = allResponses.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsSelected()))
                .count();

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

    private boolean isWithinPeriod(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        return !dateTime.isBefore(start) && !dateTime.isAfter(end);
    }

    private Double round(Double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
