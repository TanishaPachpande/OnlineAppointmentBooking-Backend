package com.medibook.auth.dto;

import lombok.*;

/**
 * Payload published to RabbitMQ.
 * The notification-service listens on medibook.otp.queue and sends the email.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpNotificationDto {
    private String recipientEmail;
    private String recipientName;
    private String otp;
    private String subject;
    private String message;
}
