package com.medibook.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Must match NotificationEventDto in notification-service field-for-field
 * so Jackson can deserialize it on the other side.
 *
 * Fields: userId, recipient, type, subject, message
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessage {

    private Long   userId;
    private String recipient;   // email address to send to
    private String type;        // "EMAIL"
    private String subject;     // email subject line
    private String message;     // email body
}
