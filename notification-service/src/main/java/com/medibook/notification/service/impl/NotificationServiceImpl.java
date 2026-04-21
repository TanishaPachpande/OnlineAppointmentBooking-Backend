package com.medibook.notification.service.impl;

import com.medibook.notification.dto.*;
import com.medibook.notification.entity.*;
import com.medibook.notification.exception.ResourceNotFoundException;
import com.medibook.notification.repository.NotificationRepository;
import com.medibook.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public NotificationResponseDto createAndSendNotification(NotificationRequestDto requestDto) {
        log.info("Creating notification for userId={} recipient={}", requestDto.getUserId(), requestDto.getRecipient());

        Notification notification = Notification.builder()
                .userId(requestDto.getUserId())
                .recipient(requestDto.getRecipient())
                .type(requestDto.getType())
                .subject(requestDto.getSubject())
                .message(requestDto.getMessage())
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);

        log.info("Notification saved and marked SENT. notificationId={}", saved.getNotificationId());

        return mapToResponse(saved);
    }

    @Override
    public NotificationResponseDto processNotificationEvent(NotificationEventDto eventDto) {
        log.info("Processing RabbitMQ notification event for userId={}", eventDto.getUserId());

        Notification notification = Notification.builder()
                .userId(eventDto.getUserId())
                .recipient(eventDto.getRecipient())
                .type(NotificationType.valueOf(eventDto.getType()))
                .subject(eventDto.getSubject())
                .message(eventDto.getMessage())
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);

        log.info("Notification event processed successfully. notificationId={}", saved.getNotificationId());

        return mapToResponse(saved);
    }

    @Override
    public NotificationResponseDto getNotificationById(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        return mapToResponse(notification);
    }

    @Override
    public List<NotificationResponseDto> getNotificationsByUser(Long userId) {
        return notificationRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<NotificationResponseDto> getAllNotifications() {
        return notificationRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private NotificationResponseDto mapToResponse(Notification notification) {
        return NotificationResponseDto.builder()
                .notificationId(notification.getNotificationId())
                .userId(notification.getUserId())
                .recipient(notification.getRecipient())
                .type(notification.getType())
                .subject(notification.getSubject())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .build();
    }
}