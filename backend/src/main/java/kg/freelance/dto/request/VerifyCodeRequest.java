package kg.freelance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kg.freelance.entity.enums.VerificationType;
import lombok.Data;

@Data
public class VerifyCodeRequest {
    @NotNull(message = "Тип верификации обязателен")
    private VerificationType type;

    @NotBlank(message = "Код обязателен")
    @Size(min = 6, max = 6, message = "Код должен содержать 6 цифр")
    private String code;
}
