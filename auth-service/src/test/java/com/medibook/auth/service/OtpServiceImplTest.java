package com.medibook.auth.service;

import com.medibook.auth.service.impl.OtpServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    private static final String TEST_USER_EMAIL = "user@example.com";
    private static final String TEST_OTP_REDIS_KEY = "auth:otp:user@example.com";
    private static final String VALID_OTP = "123456";

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 5L);
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void sendOtpStoresInRedisAndPublishesToRabbitMq() {
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        otpService.sendOtp(TEST_USER_EMAIL, "John");

        verify(valueOperations).set(eq(TEST_OTP_REDIS_KEY), anyString(), eq(Duration.ofMinutes(5)));
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void sendOtpNullNameUsesDefaultUserLabel() {
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        otpService.sendOtp(TEST_USER_EMAIL, null);

        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void verifyOtpValidOtpReturnsTrue() {
        when(valueOperations.get(TEST_OTP_REDIS_KEY)).thenReturn(VALID_OTP);

        assertThat(otpService.verifyOtp(TEST_USER_EMAIL, VALID_OTP)).isTrue();
    }

    @Test
    void verifyOtpWrongOtpReturnsFalse() {
        when(valueOperations.get(TEST_OTP_REDIS_KEY)).thenReturn(VALID_OTP);

        assertThat(otpService.verifyOtp(TEST_USER_EMAIL, "000000")).isFalse();
    }

    @Test
    void verifyOtpExpiredOrMissingReturnsFalse() {
        when(valueOperations.get(TEST_OTP_REDIS_KEY)).thenReturn(null);

        assertThat(otpService.verifyOtp(TEST_USER_EMAIL, VALID_OTP)).isFalse();
    }

    @Test
    void deleteOtpDeletesFromRedis() {
        when(redisTemplate.delete(TEST_OTP_REDIS_KEY)).thenReturn(true);

        otpService.deleteOtp(TEST_USER_EMAIL);

        verify(redisTemplate).delete(TEST_OTP_REDIS_KEY);
    }
}
