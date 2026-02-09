package kg.freelance.service;

import kg.freelance.dto.response.PaymentInitResponse;

import java.util.Map;

public interface FreedomPayService {
    PaymentInitResponse createPayment(Long userId, int days);
    String handleCallback(Map<String, String> params);
}
