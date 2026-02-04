package kg.freelance.dto.request;

import jakarta.validation.constraints.Size;
import kg.freelance.entity.enums.ProfileVisibility;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;

    @Size(max = 500, message = "WhatsApp link must not exceed 500 characters")
    private String whatsappLink;

    private ProfileVisibility profileVisibility;

    private Boolean hideFromExecutorList;
}
