package com.medibook.review.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── Must match notification-service exactly ───────────────────────────────
    public static final String EXCHANGE    = "medibook.notification.exchange";
    public static final String ROUTING_KEY = "medibook.notification.routing-key";

    @Bean
    public TopicExchange reviewNotificationExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Jackson2JsonMessageConverter reviewMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate reviewRabbitTemplate(ConnectionFactory connectionFactory,
                                               Jackson2JsonMessageConverter reviewMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(reviewMessageConverter);
        return template;
    }
}
