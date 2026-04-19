package com.medibook.provider.service;

import com.medibook.provider.dto.ProviderRequestDto;
import com.medibook.provider.dto.ProviderResponseDto;

import java.util.List;

public interface ProviderService {

    ProviderResponseDto registerProvider(ProviderRequestDto requestDto);

    ProviderResponseDto getProviderById(Long providerId);

    ProviderResponseDto getProviderByUserId(Long userId);

    List<ProviderResponseDto> getAllProviders();

    List<ProviderResponseDto> getProvidersBySpecialization(String specialization);

    List<ProviderResponseDto> searchProviders(String keyword);

    ProviderResponseDto verifyProvider(Long providerId, Boolean verified);

    ProviderResponseDto updateAvailability(Long providerId, Boolean available);

    ProviderResponseDto updateRating(Long providerId, Double avgRating);
}