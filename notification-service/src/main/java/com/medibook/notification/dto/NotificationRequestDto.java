package com.medibook.notification.dto;

import com.medibook.notification.entity.NotificationType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequestDto {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Recipient is required")
    private String recipient;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Message is required")
    private String message;
}