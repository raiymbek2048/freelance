package kg.freelance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PortfolioResponse {

    private Long id;
    private String title;
    private String description;
    private Long categoryId;
    private String categoryName;
    private List<String> images;
    private String externalLink;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
