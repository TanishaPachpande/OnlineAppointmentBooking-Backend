package com.medibook.auth.controller;

import com.medibook.auth.dto.*;
import com.medibook.auth.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j

@Tag(name = "Auth Controller", description = "APIs for registration, login and user management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
}