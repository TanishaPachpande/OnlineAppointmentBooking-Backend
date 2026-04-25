package com.medibook.notification.listener;

import com.medibook.notification.config.RabbitMqConfig;
import com.medibook.notification.dto.NotificationEventDto;
import com.medibook.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE)
    public void handleNotification(NotificationEventDto eventDto) {
        log.info("Received notification for userId={} recipient={}",
                eventDto.getUserId(), eventDto.getRecipient());
        log.info("Message: {}", eventDto.getMessage());

        notificationService.processNotificationEvent(eventDto);
    }
}