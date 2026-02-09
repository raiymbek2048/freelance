package kg.freelance.service.impl;

import kg.freelance.config.FreedomPayConfig;
import kg.freelance.dto.response.PaymentInitResponse;
import kg.freelance.entity.Payment;
import kg.freelance.entity.User;
import kg.freelance.entity.enums.PaymentStatus;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.PaymentRepository;
import kg.freelance.repository.SubscriptionSettingsRepository;
import kg.freelance.repository.UserRepository;
import kg.freelance.service.FreedomPayService;
import kg.freelance.service.InAppNotificationService;
import kg.freelance.service.SubscriptionService;
import kg.freelance.entity.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreedomPayServiceImpl implements FreedomPayService {

    private final FreedomPayConfig config;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SubscriptionSettingsRepository settingsRepository;
    private final SubscriptionService subscriptionService;
    private final InAppNotificationService inAppNotificationService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public PaymentInitResponse createPayment(Long userId, int days) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        BigDecimal price = settingsRepository.getSettings().getPrice();

        // Create payment record
        Payment payment = Payment.builder()
                .user(user)
                .amount(price)
                .days(days)
                .pgOrderId("SUB-" + UUID.randomUUID().toString().substring(0, 8))
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        // Build FreedomPay params
        String salt = UUID.randomUUID().toString();
        Map<String, String> params = new TreeMap<>();
        params.put("pg_merchant_id", config.getMerchantId());
        params.put("pg_order_id", payment.getPgOrderId());
        params.put("pg_amount", price.toPlainString());
        params.put("pg_description", "Подписка FreelanceKG на " + days + " дней");
        params.put("pg_currency", "KGS");
        params.put("pg_salt", salt);
        params.put("pg_result_url", appBaseUrl + "/api/v1/payment/result");
        params.put("pg_success_url", frontendUrl + "/payment/success");
        params.put("pg_failure_url", frontendUrl + "/payment/failure");
        params.put("pg_user_contact_email", user.getEmail());
        params.put("pg_language", "ru");

        if (config.isTestMode()) {
            params.put("pg_testing_mode", "1");
        }

        // Calculate signature
        String signature = calculateSignature("init_payment.php", params);
        params.put("pg_sig", signature);

        // Call FreedomPay API
        String apiUrl = config.getApiUrl() + "/init_payment.php";
        log.info("Calling FreedomPay init_payment: pgOrderId={}, amount={}", payment.getPgOrderId(), price);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        params.forEach(formData::add);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
        String responseBody = response.getBody();
        log.debug("FreedomPay response: {}", responseBody);

        // Parse XML response to get pg_redirect_url
        String redirectUrl = extractXmlValue(responseBody, "pg_redirect_url");
        if (redirectUrl == null || redirectUrl.isEmpty()) {
            String errorDescription = extractXmlValue(responseBody, "pg_error_description");
            log.error("FreedomPay init_payment failed: {}", errorDescription);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            throw new BadRequestException("Payment initialization failed: " + (errorDescription != null ? errorDescription : "Unknown error"));
        }

        return PaymentInitResponse.builder()
                .redirectUrl(redirectUrl)
                .paymentId(payment.getId())
                .build();
    }

    @Override
    @Transactional
    public String handleCallback(Map<String, String> params) {
        log.info("FreedomPay callback received: {}", params);

        String pgOrderId = params.get("pg_order_id");
        String pgPaymentId = params.get("pg_payment_id");
        String pgResult = params.get("pg_result");
        String pgSig = params.get("pg_sig");

        if (pgOrderId == null || pgResult == null || pgSig == null) {
            log.warn("Invalid callback: missing required params");
            return buildCallbackResponse("error", "Missing required parameters");
        }

        // Verify signature
        Map<String, String> signParams = new TreeMap<>(params);
        signParams.remove("pg_sig");
        String expectedSig = calculateSignature("result", signParams);

        if (!expectedSig.equals(pgSig)) {
            log.warn("Invalid callback signature for order {}", pgOrderId);
            return buildCallbackResponse("error", "Invalid signature");
        }

        // Find payment
        Payment payment = paymentRepository.findByPgOrderId(pgOrderId).orElse(null);
        if (payment == null) {
            log.warn("Payment not found for pgOrderId: {}", pgOrderId);
            return buildCallbackResponse("error", "Payment not found");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment {} already processed", pgOrderId);
            return buildCallbackResponse("ok", "Already processed");
        }

        if ("1".equals(pgResult)) {
            // Payment successful
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPgPaymentId(pgPaymentId);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Activate subscription
            subscriptionService.activatePaidSubscription(
                    payment.getUser().getId(),
                    payment.getDays(),
                    "FreedomPay:" + pgPaymentId
            );

            // Send notification
            inAppNotificationService.send(
                    payment.getUser(),
                    NotificationType.SUBSCRIPTION_ACTIVATED,
                    "Подписка активирована",
                    "Ваша подписка на " + payment.getDays() + " дней успешно оплачена и активирована!",
                    null,
                    "/profile"
            );

            log.info("Payment {} successful, subscription activated for user {}",
                    pgOrderId, payment.getUser().getId());
            return buildCallbackResponse("ok", "Payment processed");
        } else {
            // Payment failed
            payment.setStatus(PaymentStatus.FAILED);
            payment.setPgPaymentId(pgPaymentId);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            log.info("Payment {} failed for user {}", pgOrderId, payment.getUser().getId());
            return buildCallbackResponse("ok", "Payment failure recorded");
        }
    }

    private String calculateSignature(String scriptName, Map<String, String> params) {
        // Sort params by key (TreeMap already sorted)
        TreeMap<String, String> sorted = new TreeMap<>(params);

        StringBuilder sb = new StringBuilder();
        sb.append(scriptName);
        for (String value : sorted.values()) {
            sb.append(";").append(value);
        }
        sb.append(";").append(config.getSecretKey());

        return md5(sb.toString());
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 calculation failed", e);
        }
    }

    private String extractXmlValue(String xml, String tag) {
        if (xml == null) return null;
        Pattern pattern = Pattern.compile("<" + tag + ">(.+?)</" + tag + ">");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String buildCallbackResponse(String status, String description) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<response>\n" +
                "  <pg_status>" + status + "</pg_status>\n" +
                "  <pg_description>" + description + "</pg_description>\n" +
                "</response>";
    }
}
