package com.medibook.notification.controller;

import com.medibook.notification.dto.*;
import com.medibook.notification.messaging.NotificationProducer;
import com.medibook.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification Controller", description = "APIs for notification management")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationProducer notificationProducer;

    public NotificationController(NotificationService notificationService,
                                  NotificationProducer notificationProducer) {
        this.notificationService = notificationService;
        this.notificationProducer = notificationProducer;
    }

    @PostMapping
    public ResponseEntity<NotificationResponseDto> createNotification(
            @Valid @RequestBody NotificationRequestDto requestDto) {
        log.info("API CALL: Create notification for userId={}", requestDto.getUserId());
        return ResponseEntity.ok(notificationService.createAndSendNotification(requestDto));
    }

    @PostMapping("/publish")
    public ResponseEntity<ApiResponseDto> publishNotification(@RequestBody NotificationEventDto eventDto) {
        notificationProducer.publishNotification(eventDto);
        return ResponseEntity.ok(ApiResponseDto.builder()
                .message("Notification event published successfully")
                .build());
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponseDto> getNotificationById(@PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.getNotificationById(notificationId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponseDto>> getNotificationsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getNotificationsByUser(userId));
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }
}