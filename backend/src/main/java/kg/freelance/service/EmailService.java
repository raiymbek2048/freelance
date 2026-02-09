package kg.freelance.service;

import kg.freelance.entity.Order;
import kg.freelance.entity.User;

public interface EmailService {

    void sendExecutorSelected(User executor, Order order);

    void sendWorkSubmittedForReview(User client, Order order);

    void sendWorkApproved(User executor, Order order);

    void sendRevisionRequested(User executor, Order order, String reason);

    void sendDisputeOpened(String recipientEmail, String recipientName, String orderTitle, Long orderId, String reason);

    void sendDisputeResolved(String recipientEmail, String recipientName, String orderTitle, Long orderId, String resolution, String notes);

    void sendDisputeUnderReview(String recipientEmail, String recipientName, String orderTitle, Long orderId);

    void sendNewOrderResponse(User client, Order order, User executor);

    void sendVerificationApproved(User user);

    void sendVerificationRejected(User user, String reason);

    // Auth related emails
    void sendWelcomeEmail(User user);

    void sendEmailVerificationCode(String email, String code);

    void sendPasswordResetCode(String email, String code);
}
