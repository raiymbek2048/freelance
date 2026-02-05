package kg.freelance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.freelance.dto.request.SendCodeRequest;
import kg.freelance.dto.request.VerifyCodeRequest;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/contact-verification")
@RequiredArgsConstructor
@Tag(name = "Contact Verification", description = "Email and phone verification")
public class ContactVerificationController {

    private final VerificationCodeService verificationCodeService;

    @PostMapping("/send-code")
    @Operation(summary = "Send verification code to email or phone")
    public ResponseEntity<Map<String, String>> sendCode(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody SendCodeRequest request) {

        verificationCodeService.sendVerificationCode(currentUser.getId(), request.getType());

        String destination = request.getType().name().toLowerCase();
        return ResponseEntity.ok(Map.of(
                "message", "Код отправлен на ваш " + (destination.equals("email") ? "email" : "телефон")
        ));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify the code")
    public ResponseEntity<Map<String, Object>> verifyCode(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody VerifyCodeRequest request) {

        boolean verified = verificationCodeService.verifyCode(
                currentUser.getId(), request.getCode(), request.getType());

        if (verified) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Успешно подтверждено"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Неверный или просроченный код"
            ));
        }
    }

    @GetMapping("/can-request")
    @Operation(summary = "Check if user can request a new code")
    public ResponseEntity<Map<String, Boolean>> canRequestCode(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam String type) {

        kg.freelance.entity.enums.VerificationType verificationType =
                kg.freelance.entity.enums.VerificationType.valueOf(type.toUpperCase());

        boolean canRequest = verificationCodeService.canRequestNewCode(
                currentUser.getId(), verificationType);

        return ResponseEntity.ok(Map.of("canRequest", canRequest));
    }
}
