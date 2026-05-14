package com.medibook.appointment.messaging;

import com.medibook.appointment.config.RabbitMqConfig;
import com.medibook.appointment.dto.NotificationEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    public NotificationProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishNotification(NotificationEventDto eventDto) {
        log.info("Publishing notification event for userId={}", eventDto.getUserId());

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE,
                RabbitMqConfig.ROUTING_KEY,
                eventDto
        );

        log.info("Notification event published successfully");
    }
}