package com.medibook.notification.listener;

import com.medibook.notification.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * NEW FILE – add to notification-service.
 *
 * Listens on the medibook.otp.queue published by auth-service
 * and sends the OTP email via the existing EmailService.
 *
 * No changes needed to any existing notification-service files.
 */
@Component
@Slf4j
public class OtpNotificationListener {

    private final EmailService emailService;

    public OtpNotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * The message payload is the OtpNotificationDto serialised as JSON by auth-service.
     * Received it as a Map to avoid coupling both services to a shared DTO class.
     */
    @RabbitListener(queues = "medibook.otp.queue")
    public void handleOtpEvent(Map<String, Object> payload) {
        try {
            String recipientEmail = (String) payload.get("recipientEmail");
            String subject        = (String) payload.get("subject");
            String message        = (String) payload.get("message");

            log.info("OTP notification received for email: {}", recipientEmail);
            log.info("========================================");
            log.info("LOCAL DEV OTP MESSAGE: \n{}", message);
            log.info("========================================");
            emailService.sendEmail(recipientEmail, subject, message);
            log.info("OTP email sent successfully to: {}", recipientEmail);

        } catch (Exception e) {
            log.error("Failed to process OTP notification: {}", e.getMessage(), e);
            // Do not re-throw – prevents message from being re-queued indefinitely
        }
    }
}
