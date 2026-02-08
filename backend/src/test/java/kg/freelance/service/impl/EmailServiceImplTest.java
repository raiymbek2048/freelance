package kg.freelance.service.impl;

import jakarta.mail.internet.MimeMessage;
import kg.freelance.entity.Order;
import kg.freelance.entity.User;
import kg.freelance.entity.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailServiceImpl emailService;

    private User user;
    private User executor;
    private Order order;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@freelance.kg");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");

        user = User.builder()
                .id(1L)
                .email("client@example.com")
                .fullName("Client User")
                .role(UserRole.USER)
                .build();

        executor = User.builder()
                .id(2L)
                .email("executor@example.com")
                .fullName("Executor User")
                .role(UserRole.USER)
                .build();

        order = Order.builder()
                .id(100L)
                .title("Test Order")
                .description("Test description")
                .client(user)
                .executor(executor)
                .build();
    }

    @Nested
    @DisplayName("Email Disabled Mode Tests")
    class EmailDisabledTests {

        @BeforeEach
        void disableEmail() {
            ReflectionTestUtils.setField(emailService, "mailEnabled", false);
        }

        @Test
        @DisplayName("Should not send email when disabled - executor selected")
        void shouldNotSendEmailWhenDisabledExecutorSelected() {
            // When
            emailService.sendExecutorSelected(executor, order);

            // Then
            verify(mailSender, never()).createMimeMessage();
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should not send email when disabled - work submitted")
        void shouldNotSendEmailWhenDisabledWorkSubmitted() {
            // When
            emailService.sendWorkSubmittedForReview(user, order);

            // Then
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should not send email when disabled - work approved")
        void shouldNotSendEmailWhenDisabledWorkApproved() {
            // When
            emailService.sendWorkApproved(executor, order);

            // Then
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should not send email when disabled - revision requested")
        void shouldNotSendEmailWhenDisabledRevisionRequested() {
            // When
            emailService.sendRevisionRequested(executor, order, "Need changes");

            // Then
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should not send email when disabled - dispute opened")
        void shouldNotSendEmailWhenDisabledDisputeOpened() {
            // When
            emailService.sendDisputeOpened(user, order, "Issue");

            // Then
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should not send email when disabled - new order response")
        void shouldNotSendEmailWhenDisabledNewOrderResponse() {
            // When
            emailService.sendNewOrderResponse(user, order, executor);

            // Then
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should not send email when disabled - verification approved")
        void shouldNotSendEmailWhenDisabledVerificationApproved() {
            // When
            emailService.sendVerificationApproved(user);

            // Then
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should not send email when disabled - verification rejected")
        void shouldNotSendEmailWhenDisabledVerificationRejected() {
            // When
            emailService.sendVerificationRejected(user, "Invalid documents");

            // Then
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should not send email when disabled - welcome email")
        void shouldNotSendEmailWhenDisabledWelcomeEmail() {
            // When
            emailService.sendWelcomeEmail(user);

            // Then
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should not send email when disabled - verification code")
        void shouldNotSendEmailWhenDisabledVerificationCode() {
            // When
            emailService.sendEmailVerificationCode("test@example.com", "123456");

            // Then
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should not send email when disabled - password reset code")
        void shouldNotSendEmailWhenDisabledPasswordResetCode() {
            // When
            emailService.sendPasswordResetCode("test@example.com", "654321");

            // Then
            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("Email Enabled Mode Tests")
    class EmailEnabledTests {

        @BeforeEach
        void enableEmail() {
            ReflectionTestUtils.setField(emailService, "mailEnabled", true);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        }

        @Test
        @DisplayName("Should send executor selected email")
        void shouldSendExecutorSelectedEmail() {
            // When
            emailService.sendExecutorSelected(executor, order);

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send work submitted for review email")
        void shouldSendWorkSubmittedForReviewEmail() {
            // When
            emailService.sendWorkSubmittedForReview(user, order);

            // Then
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send work approved email")
        void shouldSendWorkApprovedEmail() {
            // When
            emailService.sendWorkApproved(executor, order);

            // Then
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send revision requested email with reason")
        void shouldSendRevisionRequestedEmailWithReason() {
            // When
            emailService.sendRevisionRequested(executor, order, "Need improvements");

            // Then
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send revision requested email without reason")
        void shouldSendRevisionRequestedEmailWithoutReason() {
            // When
            emailService.sendRevisionRequested(executor, order, null);

            // Then
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send dispute opened email")
        void shouldSendDisputeOpenedEmail() {
            // When
            emailService.sendDisputeOpened(user, order, "Issue with quality");

            // Then
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send dispute opened email with empty reason")
        void shouldSendDisputeOpenedEmailWithEmptyReason() {
            // When
            emailService.sendDisputeOpened(user, order, "");

            // Then
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send new order response email")
        void shouldSendNewOrderResponseEmail() {
            // When
            emailService.sendNewOrderResponse(user, order, executor);

            // Then
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send verification approved email")
        void shouldSendVerificationApprovedEmail() {
            // When
            emailService.sendVerificationApproved(user);

            // Then
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send verification rejected email with reason")
        void shouldSendVerificationRejectedEmailWithReason() {
            // When
            emailService.sendVerificationRejected(user, "Invalid passport");

            // Then
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send verification rejected email without reason")
        void shouldSendVerificationRejectedEmailWithoutReason() {
            // When
            emailService.sendVerificationRejected(user, null);

            // Then
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send welcome email")
        void shouldSendWelcomeEmail() {
            // When
            emailService.sendWelcomeEmail(user);

            // Then
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send email verification code")
        void shouldSendEmailVerificationCode() {
            // When
            emailService.sendEmailVerificationCode("test@example.com", "123456");

            // Then
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send password reset code")
        void shouldSendPasswordResetCode() {
            // When
            emailService.sendPasswordResetCode("test@example.com", "654321");

            // Then
            verify(mailSender).send(mimeMessage);
        }
    }

}
