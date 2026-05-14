package com.medibook.auth.service;

/**
 * Handles generation, storage (Redis), and validation of email OTPs.
 */
public interface OtpService {

    /**
     * Generate a 6-digit OTP, store it in Redis with a 5-minute TTL,
     * and publish an OTP email event to RabbitMQ.
     *
     * @param email recipient email
     * @param name  recipient name (optional, for personalised email)
     */
    void sendOtp(String email, String name);

    /**
     * Validate the OTP for the given email.
     *
     * @return true if OTP is correct and not expired
     */
    boolean verifyOtp(String email, String otp);

    /**
     * Delete OTP from Redis after successful verification (prevent reuse).
     */
    void deleteOtp(String email);
}
