package kg.freelance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ChatMessageRequest {

    @NotBlank(message = "Message content is required")
    @Size(max = 4000, message = "Message must not exceed 4000 characters")
    private String content;

    private List<String> attachments;
}
