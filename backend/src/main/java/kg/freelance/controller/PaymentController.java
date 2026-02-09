package kg.freelance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.freelance.dto.request.CreatePaymentRequest;
import kg.freelance.dto.response.PaymentInitResponse;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.FreedomPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment endpoints")
@Slf4j
public class PaymentController {

    private final FreedomPayService freedomPayService;

    @PostMapping("/subscription")
    @Operation(summary = "Create subscription payment", description = "Initialize payment for subscription via FreedomPay")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PaymentInitResponse> createSubscriptionPayment(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody CreatePaymentRequest request) {
        PaymentInitResponse response = freedomPayService.createPayment(user.getId(), request.getDays());
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/result", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "Payment callback", description = "Callback from FreedomPay (server-to-server)")
    public ResponseEntity<String> handlePaymentResult(@RequestParam Map<String, String> params) {
        String response = freedomPayService.handleCallback(params);
        return ResponseEntity.ok(response);
    }
}
