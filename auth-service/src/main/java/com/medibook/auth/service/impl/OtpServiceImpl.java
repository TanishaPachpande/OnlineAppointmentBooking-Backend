package com.medibook.auth.service.impl;

import com.medibook.auth.config.RabbitMqConfig;
import com.medibook.auth.dto.OtpNotificationDto;
import com.medibook.auth.service.OtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@Slf4j
public class OtpServiceImpl implements OtpService {

    private static final String OTP_KEY_PREFIX = "auth:otp:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.otp.expiry-minutes:5}")
    private long otpExpiryMinutes;

    @Value("${app.otp.length:6}")
    private int otpLength;

    public OtpServiceImpl(RedisTemplate<String, Object> redisTemplate,
                          RabbitTemplate rabbitTemplate) {
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void sendOtp(String email, String name) {
        String otp = generateOtp();

        // Store OTP in Redis with TTL
        String redisKey = OTP_KEY_PREFIX + email;
        redisTemplate.opsForValue().set(redisKey, otp, Duration.ofMinutes(otpExpiryMinutes));
        log.info("OTP stored in Redis for email: {} (TTL: {} min)", email, otpExpiryMinutes);

        // Publish OTP event to RabbitMQ → notification-service picks it up and sends email
        OtpNotificationDto payload = OtpNotificationDto.builder()
                .recipientEmail(email)
                .recipientName(name != null ? name : "User")
                .otp(otp)
                .subject("MediBook – Your OTP for Email Verification")
                .message(buildEmailBody(name, otp))
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.NOTIFICATION_EXCHANGE,
                RabbitMqConfig.OTP_ROUTING_KEY,
                payload
        );
        log.info("OTP notification event published to RabbitMQ for email: {}", email);
    }

    @Override
    public boolean verifyOtp(String email, String otp) {
        String redisKey = OTP_KEY_PREFIX + email;
        Object storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null) {
            log.warn("OTP not found or expired in Redis for email: {}", email);
            return false;
        }

        boolean valid = storedOtp.toString().equals(otp);
        log.info("OTP verification for email {}: {}", email, valid ? "SUCCESS" : "FAILED");
        return valid;
    }

    @Override
    public void deleteOtp(String email) {
        redisTemplate.delete(OTP_KEY_PREFIX + email);
        log.info("OTP deleted from Redis for email: {}", email);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String generateOtp() {
        int bound = (int) Math.pow(10, otpLength);          // 1_000_000 for length=6
        int otpInt = RANDOM.nextInt(bound - (int) Math.pow(10, otpLength - 1))
                + (int) Math.pow(10, otpLength - 1);        // ensures leading digits aren't 0
        return String.valueOf(otpInt);
    }

    private String buildEmailBody(String name, String otp) {
        return String.format(
                "Hello %s,\n\n" +
                        "Your MediBook OTP for email verification is: %s\n\n" +
                        "This OTP is valid for %d minutes. Do not share it with anyone.\n\n" +
                        "If you did not request this, please ignore this email.\n\n" +
                        "Regards,\nMediBook Team",
                name != null ? name : "User", otp, otpExpiryMinutes
        );
    }
}
