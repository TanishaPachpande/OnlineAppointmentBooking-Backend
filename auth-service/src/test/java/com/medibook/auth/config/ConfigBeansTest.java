package com.medibook.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ConfigBeansTest {

    // ── OpenApiConfig ─────────────────────────────────────────────────────────

    @Test
    void openApiConfig_customOpenAPI_returnsOpenAPI() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI api = config.customOpenAPI();

        assertThat(api).isNotNull();
        assertThat(api.getInfo()).isNotNull();
        assertThat(api.getInfo().getTitle()).isEqualTo("MediBook Auth Service API");
        assertThat(api.getInfo().getVersion()).isEqualTo("1.0");
    }

    // ── PasswordConfig ────────────────────────────────────────────────────────

    @Test
    void passwordConfig_passwordEncoder_returnsBCrypt() {
        PasswordConfig config = new PasswordConfig();
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder).isNotNull();
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordConfig_passwordEncoder_encodeAndMatches() {
        PasswordConfig config = new PasswordConfig();
        PasswordEncoder encoder = config.passwordEncoder();

        String encoded = encoder.encode("mypassword");
        assertThat(encoder.matches("mypassword", encoded)).isTrue();
        assertThat(encoder.matches("wrongpassword", encoded)).isFalse();
    }

    // ── RabbitMqConfig ────────────────────────────────────────────────────────

    @Test
    void rabbitMqConfig_otpQueue_isDurable() {
        RabbitMqConfig config = new RabbitMqConfig();
        Queue queue = config.otpQueue();

        assertThat(queue).isNotNull();
        assertThat(queue.getName()).isEqualTo(RabbitMqConfig.OTP_QUEUE);
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void rabbitMqConfig_notificationExchange_isTopicExchange() {
        RabbitMqConfig config = new RabbitMqConfig();
        TopicExchange exchange = config.notificationExchange();

        assertThat(exchange).isNotNull();
        assertThat(exchange.getName()).isEqualTo(RabbitMqConfig.NOTIFICATION_EXCHANGE);
    }

    @Test
    void rabbitMqConfig_otpBinding_bindsQueueToExchange() {
        RabbitMqConfig config = new RabbitMqConfig();
        Binding binding = config.otpBinding();

        assertThat(binding).isNotNull();
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitMqConfig.OTP_ROUTING_KEY);
    }

    @Test
    void rabbitMqConfig_messageConverter_isJackson() {
        RabbitMqConfig config = new RabbitMqConfig();
        Jackson2JsonMessageConverter converter = config.messageConverter();

        assertThat(converter).isNotNull();
    }

    @Test
    void rabbitMqConfig_rabbitTemplate_isNotNull() {
        RabbitMqConfig config = new RabbitMqConfig();
        ConnectionFactory factory = mock(ConnectionFactory.class);
        RabbitTemplate template = config.rabbitTemplate(factory);

        assertThat(template).isNotNull();
    }

    @Test
    void rabbitMqConfig_constants_areCorrect() {
        assertThat(RabbitMqConfig.NOTIFICATION_EXCHANGE).isEqualTo("medibook.notification.exchange");
        assertThat(RabbitMqConfig.OTP_ROUTING_KEY).isEqualTo("medibook.otp.routing-key");
        assertThat(RabbitMqConfig.OTP_QUEUE).isEqualTo("medibook.otp.queue");
    }
}
