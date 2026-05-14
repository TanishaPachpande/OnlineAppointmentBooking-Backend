package com.medibook.notification.service;

import com.medibook.notification.dto.NotificationEventDto;
import com.medibook.notification.dto.NotificationRequestDto;
import com.medibook.notification.dto.NotificationResponseDto;

import java.util.List;

public interface NotificationService {

    NotificationResponseDto createAndSendNotification(NotificationRequestDto requestDto);

    NotificationResponseDto processNotificationEvent(NotificationEventDto eventDto);

    NotificationResponseDto getNotificationById(Long notificationId);

    List<NotificationResponseDto> getNotificationsByUser(Long userId);

    List<NotificationResponseDto> getAllNotifications();
}