package kg.freelance.dto.request;

import jakarta.validation.constraints.NotNull;
import kg.freelance.entity.enums.VerificationType;
import lombok.Data;

@Data
public class SendCodeRequest {
    @NotNull(message = "Тип верификации обязателен")
    private VerificationType type;
}
