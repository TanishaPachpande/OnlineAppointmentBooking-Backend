package com.medibook.appointment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEventDto {

    private Long userId;
    private String recipient;
    private String type;
    private String subject;
    private String message;
}