package com.medibook.notification.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessage {

    private Long userId;
    private String email;
    private String message;
}