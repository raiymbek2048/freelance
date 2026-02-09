package kg.freelance.dto.response;

import kg.freelance.entity.enums.ProfileVisibility;
import kg.freelance.entity.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String phone;
    private String fullName;
    private String avatarUrl;
    private String whatsappLink;
    private ProfileVisibility profileVisibility;
    private Boolean hideFromExecutorList;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Boolean executorVerified;
    private UserRole role;
    private LocalDateTime createdAt;
    private Boolean hasExecutorProfile;
    private String bio;
}
