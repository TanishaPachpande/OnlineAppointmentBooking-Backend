package com.medibook.auth.service.impl;

import com.medibook.auth.dto.*;
import com.medibook.auth.entity.*;
import com.medibook.auth.exception.*;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.security.JwtUtil;
import com.medibook.auth.service.AuthService;
import com.medibook.auth.service.OtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_KEY_PREFIX = "auth:token:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OtpService otpService;

    @Value("${app.redis.token-ttl-hours:24}")
    private long tokenTtlHours;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           RedisTemplate<String, Object> redisTemplate,
                           OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.otpService = otpService;
    }

    @Override
    public AuthResponseDto register(RegisterRequestDto requestDto) {
        log.info("Register request received for email: {}", requestDto.getEmail());

        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new UserAlreadyExistsException("User already exists with email: " + requestDto.getEmail());
        }
        if (userRepository.existsByPhone(requestDto.getPhone())) {
            throw new UserAlreadyExistsException("User already exists with phone: " + requestDto.getPhone());
        }

        User user = User.builder()
                .fullName(requestDto.getFullName())
                .email(requestDto.getEmail())
                .passwordHash(passwordEncoder.encode(requestDto.getPassword()))
                .phone(requestDto.getPhone())
                .role(requestDto.getRole())
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name(), savedUser.getUserId());
        cacheToken(savedUser.getEmail(), token);

        log.info("User registered successfully: {}", savedUser.getEmail());

        return AuthResponseDto.builder()
                .userId(savedUser.getUserId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .token(token)
                .message("User registered successfully")
                .build();
    }

    @Override
    public AuthResponseDto registerWithOtp(RegisterWithOtpRequestDto requestDto) {
        log.info("OTP-based register request for email: {}", requestDto.getEmail());

        boolean otpValid = otpService.verifyOtp(requestDto.getEmail(), requestDto.getOtp());
        if (!otpValid) {
            throw new InvalidCredentialsException("Invalid or expired OTP for email: " + requestDto.getEmail());
        }

        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new UserAlreadyExistsException("User already exists with email: " + requestDto.getEmail());
        }
        if (userRepository.existsByPhone(requestDto.getPhone())) {
            throw new UserAlreadyExistsException("User already exists with phone: " + requestDto.getPhone());
        }

        User user = User.builder()
                .fullName(requestDto.getFullName())
                .email(requestDto.getEmail())
                .passwordHash(passwordEncoder.encode(requestDto.getPassword()))
                .phone(requestDto.getPhone())
                .role(requestDto.getRole())
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        otpService.deleteOtp(requestDto.getEmail());

        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name(), savedUser.getUserId());
        cacheToken(savedUser.getEmail(), token);

        log.info("User registered via OTP: {}", savedUser.getEmail());

        return AuthResponseDto.builder()
                .userId(savedUser.getUserId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .token(token)
                .message("User registered successfully with OTP verification")
                .build();
    }

    @Override
    public AuthResponseDto login(LoginRequestDto requestDto) {
        log.info("Login request for email: {}", requestDto.getEmail());

        // ── FIX: Always load user from DB first, verify password, THEN check cache ──
        // Old code checked Redis cache before password verification, which meant a
        // stale cached token (with old role e.g. PATIENT) was returned even after
        // the role was manually updated to ADMIN in the DB.
        User user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException("Account is deactivated");
        }

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // ── FIX: Validate cached token's role matches current DB role ──
        // If role was changed in DB (e.g. PATIENT → ADMIN), the old cached token
        // would have the wrong role — always generate a fresh token in that case.
        String token;
        String cachedToken = getCachedToken(requestDto.getEmail());

        if (cachedToken != null && jwtUtil.validateToken(cachedToken)) {
            String cachedRole = jwtUtil.extractRole(cachedToken);
            if (user.getRole().name().equals(cachedRole)) {
                // Cache hit with matching role — safe to reuse
                token = cachedToken;
                log.info("Returning cached JWT for: {}", requestDto.getEmail());
            } else {
                // Role mismatch — evict stale token and generate fresh one
                log.info("Role changed ({} → {}), regenerating JWT for: {}",
                        cachedRole, user.getRole().name(), requestDto.getEmail());
                token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getUserId());
                cacheToken(user.getEmail(), token);
            }
        } else {
            token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getUserId());
            cacheToken(user.getEmail(), token);
            log.info("New JWT generated for: {}", requestDto.getEmail());
        }

        return AuthResponseDto.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .token(token)
                .message("Login successful")
                .build();
    }

    @Override
    public AuthResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapToResponse(user, null, "User fetched successfully");
    }

    @Override
    public AuthResponseDto getUserById(Long userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mapToResponse(user, null, "User fetched successfully");
    }

    @Override
    public String deactivateAccount(Long userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setIsActive(false);
        userRepository.save(user);
        redisTemplate.delete(TOKEN_KEY_PREFIX + user.getEmail());
        log.info("User deactivated: {}", userId);
        return "Account deactivated successfully";
    }

    private void cacheToken(String email, String token) {
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + email, token, Duration.ofHours(tokenTtlHours));
    }

    private String getCachedToken(String email) {
        Object cached = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + email);
        return cached != null ? cached.toString() : null;
    }

    private AuthResponseDto mapToResponse(User user, String token, String message) {
        return AuthResponseDto.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .token(token)
                .message(message)
                .build();
    }
}
