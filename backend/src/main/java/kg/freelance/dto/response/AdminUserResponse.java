package kg.freelance.dto.response;

import kg.freelance.entity.enums.ProfileVisibility;
import kg.freelance.entity.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserResponse {

    private Long id;
    private String email;
    private String phone;
    private String fullName;
    private String avatarUrl;
    private ProfileVisibility profileVisibility;
    private Boolean hideFromExecutorList;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private UserRole role;
    private Boolean active;
    private Boolean executorVerified;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    // Executor stats (if has profile)
    private Boolean hasExecutorProfile;
    private Integer totalOrders;
    private Integer completedOrders;
    private BigDecimal rating;
    private Integer reviewCount;

    // Client stats
    private Integer ordersAsClient;
    private Integer ordersAsExecutor;
}
