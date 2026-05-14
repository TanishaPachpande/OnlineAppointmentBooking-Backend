package com.medibook.auth.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for auth-service.
 *
 * Reuses the same broker as appointment-service.
 * The auth-service publishes OTP events to the existing notification exchange
 * so the notification-service sends the OTP email — no new queues/exchanges needed.
 *
 * Exchange  : medibook.notification.exchange   (already declared by notification-service)
 * Routing key: medibook.otp.routing-key        (new routing key, same exchange)
 * Queue      : medibook.otp.queue              (consumed by notification-service)
 */
@Configuration
public class RabbitMqConfig {

    // Reuse notification exchange (already exists)
    public static final String NOTIFICATION_EXCHANGE = "medibook.notification.exchange";

    // New routing key for OTP emails
    public static final String OTP_ROUTING_KEY = "medibook.otp.routing-key";
    public static final String OTP_QUEUE = "medibook.otp.queue";

    @Bean
    public Queue otpQueue() {
        return new Queue(OTP_QUEUE, true);
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Binding otpBinding() {
        return BindingBuilder
                .bind(otpQueue())
                .to(notificationExchange())
                .with(OTP_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
