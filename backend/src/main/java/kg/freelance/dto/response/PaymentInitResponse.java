package kg.freelance.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitResponse {
    private String redirectUrl;
    private Long paymentId;
}
