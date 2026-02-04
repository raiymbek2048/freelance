package kg.freelance.dto.response;

import kg.freelance.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderListResponse {

    private Long id;
    private String title;
    private Long categoryId;
    private String categoryName;
    private Long clientId;
    private String clientName;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private LocalDate deadline;
    private OrderStatus status;
    private Integer responseCount;
    private LocalDateTime createdAt;
}
