package com.medibook.auth.controller;

import com.medibook.auth.dto.*;
import com.medibook.auth.service.AuthService;
import com.medibook.auth.service.OtpService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REPLACES the original AuthController.
 *
 * Original endpoints:
 *  GET  /auth/test
 *  POST /auth/register
 *  POST /auth/login
 *  GET  /auth/profile/email/{email}
 *  GET  /auth/profile/{userId}
 *  PUT  /auth/deactivate/{userId}
 *
 * New endpoints added:
 *  POST /auth/send-otp          – generate & send OTP to email
 *  POST /auth/verify-otp        – verify OTP (returns success/failure, no token)
 *  POST /auth/register-with-otp – full registration after OTP verified
 */
@RestController
@RequestMapping("/auth")
@Slf4j
@Tag(name = "Auth Controller", description = "APIs for registration, login, OTP and user management")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }


    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth working");
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto requestDto) {
        log.info("Received registration request");
        return ResponseEntity.ok(authService.register(requestDto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto) {
        log.info("Received login request");
        return ResponseEntity.ok(authService.login(requestDto));
    }

    @GetMapping("/profile/email/{email}")
    public ResponseEntity<AuthResponseDto> getByEmail(@PathVariable String email) {
        return ResponseEntity.ok(authService.getUserByEmail(email));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<AuthResponseDto> getById(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @PutMapping("/deactivate/{userId}")
    public ResponseEntity<ApiResponseDto> deactivate(@PathVariable Long userId) {
        return ResponseEntity.ok(
                ApiResponseDto.builder()
                        .message(authService.deactivateAccount(userId))
                        .build()
        );
    }


    /**
     * Step 1 – Send a 6-digit OTP to the provided email via RabbitMQ → notification-service.
     * The OTP is stored in Redis with a 5-minute TTL.
     *
     * POST /auth/send-otp
     * Body: { "email": "user@example.com" }
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponseDto> sendOtp(@Valid @RequestBody SendOtpRequestDto requestDto) {
        log.info("OTP send request for email: {}", requestDto.getEmail());
        otpService.sendOtp(requestDto.getEmail(), null);
        return ResponseEntity.ok(
                ApiResponseDto.builder()
                        .message("OTP sent successfully to " + requestDto.getEmail())
                        .build()
        );
    }

    /**
     * Step 2a – Verify OTP only (used when you want to gate an action without registering yet).
     *
     * POST /auth/verify-otp
     * Body: { "email": "user@example.com", "otp": "123456" }
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponseDto> verifyOtp(@Valid @RequestBody VerifyOtpRequestDto requestDto) {
        log.info("OTP verify request for email: {}", requestDto.getEmail());
        boolean valid = otpService.verifyOtp(requestDto.getEmail(), requestDto.getOtp());
        if (!valid) {
            return ResponseEntity.badRequest().body(
                    ApiResponseDto.builder().message("Invalid or expired OTP").build()
            );
        }
        return ResponseEntity.ok(
                ApiResponseDto.builder().message("OTP verified successfully").build()
        );
    }

    /**
     * Step 2b – Complete registration after OTP is verified.
     * This combines OTP verification + account creation in one call.
     *
     * POST /auth/register-with-otp
     * Body: { fullName, email, password, phone, role, otp }
     */
    @PostMapping("/register-with-otp")
    public ResponseEntity<AuthResponseDto> registerWithOtp(
            @Valid @RequestBody RegisterWithOtpRequestDto requestDto) {
        log.info("OTP-based registration request for email: {}", requestDto.getEmail());
        return ResponseEntity.ok(authService.registerWithOtp(requestDto));
    }
}
