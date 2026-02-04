package kg.freelance.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class ExecutorProfileRequest {

    @Size(max = 2000, message = "Bio must not exceed 2000 characters")
    private String bio;

    @Size(max = 200, message = "Specialization must not exceed 200 characters")
    private String specialization;

    private Set<Long> categoryIds;

    private Boolean availableForWork;
}
