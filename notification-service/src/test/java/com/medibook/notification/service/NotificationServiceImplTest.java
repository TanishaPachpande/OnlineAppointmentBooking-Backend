package com.medibook.notification.service;

import com.medibook.notification.dto.NotificationEventDto;
import com.medibook.notification.dto.NotificationRequestDto;
import com.medibook.notification.dto.NotificationResponseDto;
import com.medibook.notification.entity.Notification;
import com.medibook.notification.entity.NotificationStatus;
import com.medibook.notification.entity.NotificationType;
import com.medibook.notification.exception.ResourceNotFoundException;
import com.medibook.notification.repository.NotificationRepository;
import com.medibook.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private EmailService emailService;
    @InjectMocks private NotificationServiceImpl notificationService;

    private Notification sentNotification;
    private NotificationRequestDto emailRequest;

    @BeforeEach
    void setUp() {
        sentNotification = Notification.builder()
                .notificationId(1L).userId(100L).recipient("user@example.com")
                .type(NotificationType.EMAIL).subject("Test Subject").message("Test message")
                .status(NotificationStatus.SENT).createdAt(LocalDateTime.now())
                .sentAt(LocalDateTime.now()).build();

        emailRequest = NotificationRequestDto.builder()
                .userId(100L).recipient("user@example.com")
                .type(NotificationType.EMAIL).subject("Test Subject").message("Test message").build();
    }

    // ── createAndSendNotification ──────────────────────────────────────────────

    @Test
    void createAndSendNotification_emailSuccess_statusSent() {
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());
        when(notificationRepository.save(any())).thenReturn(sentNotification);

        NotificationResponseDto result = notificationService.createAndSendNotification(emailRequest);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(emailService).sendEmail("user@example.com", "Test Subject", "Test message");
    }

    @Test
    void createAndSendNotification_emailFails_statusFailed_noThrow() {
        Notification failedNotif = Notification.builder()
                .notificationId(2L).userId(100L).recipient("user@example.com")
                .type(NotificationType.EMAIL).subject("Test Subject").message("Test message")
                .status(NotificationStatus.FAILED).createdAt(LocalDateTime.now()).build();

        doThrow(new MailSendException("SMTP error")).when(emailService).sendEmail(anyString(), anyString(), anyString());
        when(notificationRepository.save(any())).thenReturn(failedNotif);

        NotificationResponseDto result = notificationService.createAndSendNotification(emailRequest);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(notificationRepository).save(any());
    }

    // ── processNotificationEvent ───────────────────────────────────────────────

    @Test
    void processNotificationEvent_emailSuccess() {
        NotificationEventDto event = NotificationEventDto.builder()
                .userId(100L).recipient("user@example.com")
                .type("EMAIL").subject("Event Subject").message("Event message").build();

        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());
        when(notificationRepository.save(any())).thenReturn(sentNotification);

        NotificationResponseDto result = notificationService.processNotificationEvent(event);

        assertThat(result).isNotNull();
        verify(emailService).sendEmail("user@example.com", "Event Subject", "Event message");
    }

    @Test
    void processNotificationEvent_unknownType_fallsBackToEmail() {
        NotificationEventDto event = NotificationEventDto.builder()
                .userId(100L).recipient("user@example.com")
                .type("UNKNOWN_TYPE").subject("Subject").message("Message").build();

        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());
        when(notificationRepository.save(any())).thenReturn(sentNotification);

        // Should NOT throw — falls back to EMAIL type
        NotificationResponseDto result = notificationService.processNotificationEvent(event);
        assertThat(result).isNotNull();
    }

    @Test
    void processNotificationEvent_emailFails_savedAsFailed_noRethrow() {
        NotificationEventDto event = NotificationEventDto.builder()
                .userId(100L).recipient("user@example.com")
                .type("EMAIL").subject("Subject").message("Message").build();

        Notification failed = Notification.builder()
                .notificationId(3L).userId(100L).status(NotificationStatus.FAILED)
                .createdAt(LocalDateTime.now()).build();

        doThrow(new MailSendException("SMTP error")).when(emailService).sendEmail(anyString(), anyString(), anyString());
        when(notificationRepository.save(any())).thenReturn(failed);

        // Must NOT throw (prevents RabbitMQ requeue loop)
        NotificationResponseDto result = notificationService.processNotificationEvent(event);
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    // ── getNotificationById ────────────────────────────────────────────────────

    @Test
    void getNotificationById_found() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sentNotification));
        assertThat(notificationService.getNotificationById(1L).getRecipient()).isEqualTo("user@example.com");
    }

    @Test
    void getNotificationById_notFound_throws() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> notificationService.getNotificationById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getNotificationsByUser ─────────────────────────────────────────────────

    @Test
    void getNotificationsByUser_returnsList() {
        when(notificationRepository.findByUserId(100L)).thenReturn(List.of(sentNotification));
        assertThat(notificationService.getNotificationsByUser(100L)).hasSize(1);
    }

    @Test
    void getNotificationsByUser_nullUserId_returnsEmpty() {
        assertThat(notificationService.getNotificationsByUser(null)).isEmpty();
        verify(notificationRepository, never()).findByUserId(any());
    }

    // ── getAllNotifications ────────────────────────────────────────────────────

    @Test
    void getAllNotifications_returnsList() {
        when(notificationRepository.findAll()).thenReturn(List.of(sentNotification, sentNotification));
        assertThat(notificationService.getAllNotifications()).hasSize(2);
    }
}
