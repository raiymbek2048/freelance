package kg.freelance.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnnouncementResponse {

    private String message;
    private Boolean enabled;
}
