package com.medibook.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Matches NotificationEventDto in notification-service exactly.
 * Fields: userId, recipient, type, subject, message
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessage {

    private Long   userId;
    private String recipient;   // email address
    private String type;        // "EMAIL"
    private String subject;     // email subject
    private String message;     // email body
}
