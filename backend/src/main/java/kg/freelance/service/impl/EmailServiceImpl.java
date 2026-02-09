package kg.freelance.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import kg.freelance.entity.Order;
import kg.freelance.entity.User;
import kg.freelance.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@freelance.kg}")
    private String fromAddress;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    @Async("emailExecutor")
    public void sendExecutorSelected(User executor, Order order) {
        String subject = "Вас выбрали исполнителем!";
        String message = String.format(
            "Поздравляем, %s! Вас выбрали исполнителем для заказа \"%s\". " +
            "Свяжитесь с заказчиком и приступайте к работе.",
            executor.getFullName(), order.getTitle()
        );
        String buttonUrl = frontendUrl + "/orders/" + order.getId();

        sendEmail(executor.getEmail(), subject, message, "Открыть заказ", buttonUrl);
    }

    @Override
    @Async("emailExecutor")
    public void sendWorkSubmittedForReview(User client, Order order) {
        String subject = "Работа отправлена на проверку";
        String message = String.format(
            "%s, исполнитель завершил работу над заказом \"%s\" и отправил её на проверку. " +
            "Пожалуйста, проверьте результат и примите решение.",
            client.getFullName(), order.getTitle()
        );
        String buttonUrl = frontendUrl + "/orders/" + order.getId();

        sendEmail(client.getEmail(), subject, message, "Проверить работу", buttonUrl);
    }

    @Override
    @Async("emailExecutor")
    public void sendWorkApproved(User executor, Order order) {
        String subject = "Ваша работа одобрена!";
        String message = String.format(
            "Отличная работа, %s! Заказчик одобрил вашу работу по заказу \"%s\". " +
            "Спасибо за качественное выполнение!",
            executor.getFullName(), order.getTitle()
        );
        String buttonUrl = frontendUrl + "/orders/" + order.getId();

        sendEmail(executor.getEmail(), subject, message, "Посмотреть", buttonUrl);
    }

    @Override
    @Async("emailExecutor")
    public void sendRevisionRequested(User executor, Order order, String reason) {
        String subject = "Требуется доработка";
        String reasonText = reason != null && !reason.isEmpty()
            ? "Причина: " + reason
            : "Заказчик не указал причину.";
        String message = String.format(
            "%s, заказчик запросил доработку по заказу \"%s\". %s " +
            "Пожалуйста, внесите необходимые изменения.",
            executor.getFullName(), order.getTitle(), reasonText
        );
        String buttonUrl = frontendUrl + "/orders/" + order.getId();

        sendEmail(executor.getEmail(), subject, message, "Открыть заказ", buttonUrl);
    }

    @Override
    @Async("emailExecutor")
    public void sendDisputeOpened(String recipientEmail, String recipientName, String orderTitle, Long orderId, String reason) {
        String subject = "Открыт спор по заказу";
        String reasonText = reason != null && !reason.isEmpty()
            ? "Причина: " + reason
            : "";
        String message = String.format(
            "%s, по заказу \"%s\" был открыт спор. %s " +
            "Администрация рассмотрит ситуацию и примет решение.",
            recipientName, orderTitle, reasonText
        );
        String buttonUrl = frontendUrl + "/orders/" + orderId;

        sendEmail(recipientEmail, subject, message, "Подробнее", buttonUrl);
    }

    @Override
    @Async("emailExecutor")
    public void sendDisputeResolved(String recipientEmail, String recipientName, String orderTitle, Long orderId, String resolution, String notes) {
        String subject = "Спор разрешён";
        String notesText = notes != null && !notes.isEmpty() ? "\nКомментарий: " + notes : "";
        String message = String.format(
            "%s, спор по заказу \"%s\" был разрешён %s.%s",
            recipientName, orderTitle, resolution, notesText
        );
        String buttonUrl = frontendUrl + "/orders/" + orderId + "/dispute";

        sendEmail(recipientEmail, subject, message, "Подробнее", buttonUrl);
    }

    @Override
    @Async("emailExecutor")
    public void sendDisputeUnderReview(String recipientEmail, String recipientName, String orderTitle, Long orderId) {
        String subject = "Спор принят на рассмотрение";
        String message = String.format(
            "%s, спор по заказу \"%s\" принят модератором на рассмотрение. " +
            "Вы получите уведомление, когда решение будет принято.",
            recipientName, orderTitle
        );
        String buttonUrl = frontendUrl + "/orders/" + orderId + "/dispute";

        sendEmail(recipientEmail, subject, message, "Подробнее", buttonUrl);
    }

    @Override
    @Async("emailExecutor")
    public void sendNewOrderResponse(User client, Order order, User executor) {
        String subject = "Новый отклик на заказ";
        String message = String.format(
            "%s, на ваш заказ \"%s\" откликнулся исполнитель %s. " +
            "Посмотрите профиль исполнителя и примите решение.",
            client.getFullName(), order.getTitle(), executor.getFullName()
        );
        String buttonUrl = frontendUrl + "/orders/" + order.getId();

        sendEmail(client.getEmail(), subject, message, "Посмотреть отклики", buttonUrl);
    }

    @Override
    @Async("emailExecutor")
    public void sendVerificationApproved(User user) {
        String subject = "Верификация пройдена!";
        String message = String.format(
            "Поздравляем, %s! Ваша верификация успешно пройдена. " +
            "Теперь вы можете откликаться на заказы и видеть полную информацию.",
            user.getFullName()
        );
        String buttonUrl = frontendUrl + "/orders";

        sendEmail(user.getEmail(), subject, message, "Перейти к заказам", buttonUrl);
    }

    @Override
    @Async("emailExecutor")
    public void sendVerificationRejected(User user, String reason) {
        String subject = "Верификация отклонена";
        String reasonText = reason != null && !reason.isEmpty()
            ? "Причина: " + reason
            : "Причина не указана.";
        String message = String.format(
            "%s, к сожалению, ваша заявка на верификацию была отклонена. %s " +
            "Вы можете подать заявку повторно, исправив указанные недочёты.",
            user.getFullName(), reasonText
        );
        String buttonUrl = frontendUrl + "/verification";

        sendEmail(user.getEmail(), subject, message, "Подать повторно", buttonUrl);
    }

    @Override
    @Async("emailExecutor")
    public void sendWelcomeEmail(User user) {
        String subject = "Добро пожаловать в FreelanceKG!";
        String message = String.format(
            "Здравствуйте, %s! Добро пожаловать на FreelanceKG - " +
            "фриланс платформу Кыргызстана. Теперь вы можете размещать заказы " +
            "или предлагать свои услуги как исполнитель.",
            user.getFullName()
        );
        String buttonUrl = frontendUrl + "/orders";

        sendEmail(user.getEmail(), subject, message, "Начать работу", buttonUrl);
    }

    @Override
    @Async("emailExecutor")
    public void sendEmailVerificationCode(String email, String code) {
        String subject = "Код подтверждения email";
        String message = String.format(
            "Ваш код подтверждения: <strong style=\"font-size: 24px; letter-spacing: 3px;\">%s</strong><br><br>" +
            "Код действителен 10 минут. Если вы не запрашивали этот код, проигнорируйте это письмо.",
            code
        );
        String buttonUrl = frontendUrl + "/profile";

        sendEmail(email, subject, message, "Перейти в профиль", buttonUrl);
    }

    @Override
    @Async("emailExecutor")
    public void sendPasswordResetCode(String email, String code) {
        String subject = "Сброс пароля";
        String message = String.format(
            "Вы запросили сброс пароля. Ваш код подтверждения: " +
            "<strong style=\"font-size: 24px; letter-spacing: 3px;\">%s</strong><br><br>" +
            "Код действителен 10 минут. Если вы не запрашивали сброс пароля, " +
            "проигнорируйте это письмо и ваш пароль останется прежним.",
            code
        );
        String buttonUrl = frontendUrl + "/login";

        sendEmail(email, subject, message, "Войти", buttonUrl);
    }

    private void sendEmail(String to, String subject, String message, String buttonText, String buttonUrl) {
        if (!mailEnabled) {
            log.debug("Email disabled. Would send to {}: {}", to, subject);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(buildEmailTemplate(subject, message, buttonText, buttonUrl), true);

            mailSender.send(mimeMessage);
            log.info("Email sent successfully to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildEmailTemplate(String title, String message, String buttonText, String buttonUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff;">
                    <div style="background: linear-gradient(135deg, #06b6d4 0%%, #0891b2 100%%); padding: 30px 20px; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 28px;">FreelanceKG</h1>
                        <p style="color: rgba(255,255,255,0.9); margin: 5px 0 0 0; font-size: 14px;">Фриланс биржа Кыргызстана</p>
                    </div>
                    <div style="padding: 40px 30px;">
                        <h2 style="color: #1f2937; margin: 0 0 20px 0; font-size: 22px;">%s</h2>
                        <p style="color: #4b5563; line-height: 1.7; font-size: 16px; margin: 0 0 30px 0;">%s</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #06b6d4 0%%, #0891b2 100%%); color: white; padding: 14px 35px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px;">%s</a>
                        </div>
                    </div>
                    <div style="padding: 25px; text-align: center; background-color: #f9fafb; border-top: 1px solid #e5e7eb;">
                        <p style="color: #9ca3af; font-size: 13px; margin: 0;">
                            FreelanceKG - Платформа для фрилансеров Кыргызстана
                        </p>
                        <p style="color: #9ca3af; font-size: 12px; margin: 10px 0 0 0;">
                            Это автоматическое сообщение, не отвечайте на него.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(title, message, buttonUrl, buttonText);
    }
}
