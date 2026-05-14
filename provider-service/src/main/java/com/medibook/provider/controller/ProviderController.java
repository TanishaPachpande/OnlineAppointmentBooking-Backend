package com.medibook.provider.controller;

import com.medibook.provider.dto.*;
import com.medibook.provider.service.ProviderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/providers")
@Tag(name = "Provider Controller", description = "APIs for provider management")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @PostMapping
    public ResponseEntity<ProviderResponseDto> registerProvider(@Valid @RequestBody ProviderRequestDto requestDto) {
        log.info("API CALL: Register Provider - userId={}", requestDto.getUserId());
        return ResponseEntity.ok(providerService.registerProvider(requestDto));
    }

    @GetMapping("/{providerId}")
    public ResponseEntity<ProviderResponseDto> getProviderById(@PathVariable Long providerId) {
        log.info("API CALL: Get Provider by ID - {}", providerId);
        return ResponseEntity.ok(providerService.getProviderById(providerId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ProviderResponseDto> getProviderByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(providerService.getProviderByUserId(userId));
    }

    @GetMapping
    public ResponseEntity<List<ProviderResponseDto>> getAllProviders() {
        log.info("API CALL: Get All Providers");
        return ResponseEntity.ok(providerService.getAllProviders());
    }

    @GetMapping("/specialization/{specialization}")
    public ResponseEntity<List<ProviderResponseDto>> getBySpecialization(@PathVariable String specialization) {
        return ResponseEntity.ok(providerService.getProvidersBySpecialization(specialization));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProviderResponseDto>> searchProviders(@RequestParam String keyword) {
        return ResponseEntity.ok(providerService.searchProviders(keyword));
    }

    /**
     * Returns all providers whose verificationStatus = PENDING.
     * Used by the admin dashboard to show the approval queue.
     */
    @GetMapping("/pending-verification")
    public ResponseEntity<List<ProviderResponseDto>> getPendingProviders() {
        log.info("API CALL: Get Pending Providers");
        return ResponseEntity.ok(providerService.getPendingProviders());
    }

    /**
     * Admin: approve or reject a provider with an optional note.
     * Body: { "approved": true/false, "note": "optional reason" }
     */
    @PutMapping("/{providerId}/review")
    public ResponseEntity<ProviderResponseDto> reviewProvider(
            @PathVariable Long providerId,
            @Valid @RequestBody ProviderVerificationActionDto actionDto) {
        log.info("API CALL: Admin Review Provider - {}, approved={}", providerId, actionDto.getApproved());
        return ResponseEntity.ok(providerService.reviewProvider(providerId, actionDto));
    }

    /** Legacy endpoint – kept for backward compatibility */
    @PutMapping("/{providerId}/verify")
    public ResponseEntity<ProviderResponseDto> verifyProvider(@PathVariable Long providerId,
                                                              @RequestParam Boolean verified) {
        return ResponseEntity.ok(providerService.verifyProvider(providerId, verified));
    }

    @PutMapping("/{providerId}/availability")
    public ResponseEntity<ProviderResponseDto> updateAvailability(@PathVariable Long providerId,
                                                                  @RequestParam Boolean available) {
        return ResponseEntity.ok(providerService.updateAvailability(providerId, available));
    }

    @PutMapping("/{providerId}/rating")
    public ResponseEntity<ProviderResponseDto> updateRating(@PathVariable Long providerId,
                                                            @RequestParam Double avgRating) {
        return ResponseEntity.ok(providerService.updateRating(providerId, avgRating));
    }

    @PutMapping("/{providerId}")
    public ResponseEntity<ProviderResponseDto> updateProvider(@PathVariable Long providerId,
                                                              @Valid @RequestBody ProviderRequestDto requestDto) {
        log.info("API CALL: Update Provider - {}", providerId);
        return ResponseEntity.ok(providerService.updateProvider(providerId, requestDto));
    }
}