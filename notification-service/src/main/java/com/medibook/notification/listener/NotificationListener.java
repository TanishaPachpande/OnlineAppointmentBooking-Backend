package com.medibook.notification.listener;

import com.medibook.notification.dto.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationListener {

    @RabbitListener(queues = "notification.queue")
    public void consume(NotificationMessage message) {
        log.info("Received notification for userId={} email={}",
                message.getUserId(), message.getEmail());

        log.info("Message: {}", message.getMessage());

        // Later: send email
    }
}