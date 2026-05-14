package com.medibook.provider.service;

import com.medibook.provider.dto.ProviderRequestDto;
import com.medibook.provider.dto.ProviderResponseDto;
import com.medibook.provider.dto.ProviderVerificationActionDto;
import jakarta.validation.Valid;

import java.util.List;

public interface ProviderService {

    ProviderResponseDto registerProvider(ProviderRequestDto requestDto);

    ProviderResponseDto getProviderById(Long providerId);

    ProviderResponseDto getProviderByUserId(Long userId);

    List<ProviderResponseDto> getAllProviders();

    List<ProviderResponseDto> getProvidersBySpecialization(String specialization);

    List<ProviderResponseDto> searchProviders(String keyword);

    /** Legacy toggle used by old code – kept for backward compat */
    ProviderResponseDto verifyProvider(Long providerId, Boolean verified);

    /**
     * Admin approve / reject with optional note.
     * Sets verificationStatus = APPROVED/REJECTED and isVerified accordingly.
     */
    ProviderResponseDto reviewProvider(Long providerId, ProviderVerificationActionDto actionDto);

    /** Returns all providers whose verificationStatus = PENDING */
    List<ProviderResponseDto> getPendingProviders();

    ProviderResponseDto updateAvailability(Long providerId, Boolean available);

    ProviderResponseDto updateRating(Long providerId, Double avgRating);

    ProviderResponseDto updateProvider(Long providerId, ProviderRequestDto requestDto);
}