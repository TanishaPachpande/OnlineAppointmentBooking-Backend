package com.medibook.notification.service.impl;

import com.medibook.notification.dto.NotificationEventDto;
import com.medibook.notification.dto.NotificationRequestDto;
import com.medibook.notification.dto.NotificationResponseDto;
import com.medibook.notification.entity.Notification;
import com.medibook.notification.entity.NotificationStatus;
import com.medibook.notification.entity.NotificationType;
import com.medibook.notification.exception.ResourceNotFoundException;
import com.medibook.notification.repository.NotificationRepository;
import com.medibook.notification.service.EmailService;
import com.medibook.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    @Override
    public NotificationResponseDto createAndSendNotification(NotificationRequestDto requestDto) {
        log.info("Creating notification for userId={} recipient={}",
                requestDto.getUserId(), requestDto.getRecipient());

        Notification notification = Notification.builder()
                .userId(requestDto.getUserId())
                .recipient(requestDto.getRecipient())
                .type(requestDto.getType())
                .subject(requestDto.getSubject())
                .message(requestDto.getMessage())
                .status(NotificationStatus.PENDING)
                .build();

        try {
            sendNotification(
                    requestDto.getType(),
                    requestDto.getRecipient(),
                    requestDto.getSubject(),
                    requestDto.getMessage()
            );

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());

            log.info("Notification sent successfully for userId={}", requestDto.getUserId());
        } catch (Exception ex) {
            notification.setStatus(NotificationStatus.FAILED);
            log.error("Failed to send notification for userId={}: {}",
                    requestDto.getUserId(), ex.getMessage(), ex);
        }

        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved. notificationId={}, status={}",
                saved.getNotificationId(), saved.getStatus());

        return mapToResponse(saved);
    }

    @Override
    public NotificationResponseDto processNotificationEvent(NotificationEventDto eventDto) {
        log.info("Processing RabbitMQ notification event for userId={}", eventDto.getUserId());

        NotificationType notificationType = NotificationType.valueOf(eventDto.getType().toUpperCase());

        Notification notification = Notification.builder()
                .userId(eventDto.getUserId())
                .recipient(eventDto.getRecipient())
                .type(notificationType)
                .subject(eventDto.getSubject())
                .message(eventDto.getMessage())
                .status(NotificationStatus.PENDING)
                .build();

        try {
            sendNotification(
                    notificationType,
                    eventDto.getRecipient(),
                    eventDto.getSubject(),
                    eventDto.getMessage()
            );

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());

            log.info("Notification event processed successfully for userId={}", eventDto.getUserId());
        } catch (Exception ex) {
            notification.setStatus(NotificationStatus.FAILED);
            log.error("Failed to process notification event for userId={}: {}",
                    eventDto.getUserId(), ex.getMessage(), ex);
        }

        Notification saved = notificationRepository.save(notification);
        log.info("Notification event saved. notificationId={}, status={}",
                saved.getNotificationId(), saved.getStatus());

        return mapToResponse(saved);
    }

    @Override
    public NotificationResponseDto getNotificationById(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Notification not found with id: " + notificationId));

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

    private void sendNotification(NotificationType type, String recipient, String subject, String message) {
        if (type == NotificationType.EMAIL) {
            log.info("Sending EMAIL notification to {}", recipient);
            emailService.sendEmail(recipient, subject, message);
        } else {
            throw new IllegalArgumentException("Only EMAIL notification is supported currently: " + type);
        }
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