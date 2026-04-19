package com.medibook.auth.service.impl;

import com.medibook.auth.dto.*;
import com.medibook.auth.entity.*;
import com.medibook.auth.exception.*;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.security.JwtUtil;
import com.medibook.auth.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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
        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        log.info("User registered successfully with email: {}", savedUser.getEmail());

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
    public AuthResponseDto login(LoginRequestDto requestDto) {
        log.info("Login request received for email: {}", requestDto.getEmail());

        User user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException("Account is deactivated");
        }

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        log.info("Login successful for email: {}", user.getEmail());

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

        log.info("User deactivated successfully with id: {}", userId);
        return "Account deactivated successfully";
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