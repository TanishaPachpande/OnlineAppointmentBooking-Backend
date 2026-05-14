package com.medibook.review.messaging;

import com.medibook.review.config.RabbitMQConfig;
import com.medibook.review.dto.NotificationMessage;
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

    public void publishNotification(NotificationMessage msg) {
        log.info("Publishing review notification to exchange={} routingKey={} for recipient={}",
                RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, msg.getRecipient());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                msg
        );

        log.info("Review notification published successfully");
    }
}
