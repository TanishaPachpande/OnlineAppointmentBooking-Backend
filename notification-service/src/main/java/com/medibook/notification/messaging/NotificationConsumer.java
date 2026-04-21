package com.medibook.notification.messaging;

import com.medibook.notification.config.RabbitMqConfig;
import com.medibook.notification.dto.NotificationEventDto;
import com.medibook.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    public NotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE)
    public void consumeNotification(NotificationEventDto eventDto) {
        log.info("Received RabbitMQ notification event for userId={}", eventDto.getUserId());
        notificationService.processNotificationEvent(eventDto);
    }
}