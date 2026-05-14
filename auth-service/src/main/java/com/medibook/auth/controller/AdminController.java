package com.medibook.auth.controller;

import com.medibook.auth.dto.AuthResponseDto;
import com.medibook.auth.entity.Role;
import com.medibook.auth.entity.User;
import com.medibook.auth.exception.ResourceNotFoundException;
import com.medibook.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequestMapping("/auth/admin")
@Slf4j
@Tag(name = "Admin Controller", description = "Admin APIs for user management")
public class AdminController {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    // ── Reads provider-service base URL from application.yml / application.properties ──
    // Add this to your auth-service application.yml:
    //   provider:
    //     service:
    //       url: http://localhost:8083
    @Value("${provider.service.url:http://localhost:8083}")
    private String providerServiceUrl;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.restTemplate = new RestTemplate();
    }

    // GET all users
    @GetMapping("/users")
    public ResponseEntity<List<AuthResponseDto>> getAllUsers() {
        log.info("Admin: fetching all users");
        List<AuthResponseDto> users = userRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
        return ResponseEntity.ok(users);
    }

    // GET users by role
    @GetMapping("/users/role/{role}")
    public ResponseEntity<List<AuthResponseDto>> getUsersByRole(@PathVariable Role role) {
        log.info("Admin: fetching users with role={}", role);
        List<AuthResponseDto> users = userRepository.findAllByRole(role)
                .stream()
                .map(this::mapToDto)
                .toList();
        return ResponseEntity.ok(users);
    }

    // PUT activate user
    @PutMapping("/users/{userId}/activate")
    public ResponseEntity<String> activateUser(@PathVariable Long userId) {
        log.info("Admin: activating userId={}", userId);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setIsActive(true);
        userRepository.save(user);
        return ResponseEntity.ok("User activated successfully");
    }

    // PUT deactivate user
    @PutMapping("/users/{userId}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable Long userId) {
        log.info("Admin: deactivating userId={}", userId);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setIsActive(false);
        userRepository.save(user);
        return ResponseEntity.ok("User deactivated successfully");
    }

    // PUT verify provider — delegates to provider-service
    // ── FIX: This now actually calls provider-service:8083 to set isVerified=true ──
    // Make sure your provider-service has: PUT /providers/{providerId}/verify
    @PutMapping("/users/providers/{providerId}/verify")
    public ResponseEntity<String> verifyProvider(
            @PathVariable Long providerId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Admin: verifying providerId={}", providerId);
        try {
            HttpHeaders headers = new HttpHeaders();
            if (authHeader != null) headers.set("Authorization", authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Forward the request to provider-service
            ResponseEntity<String> providerResponse = restTemplate.exchange(
                    providerServiceUrl + "/providers/" + providerId + "/verify",
                    HttpMethod.PUT,
                    entity,
                    String.class
            );
            return ResponseEntity.ok("Provider verified successfully");
        } catch (Exception e) {
            log.error("Failed to verify provider {}: {}", providerId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to verify provider: " + e.getMessage());
        }
    }

    private AuthResponseDto mapToDto(User user) {
        return AuthResponseDto.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .message(user.getIsActive() ? "ACTIVE" : "INACTIVE")
                .build();
    }
}
