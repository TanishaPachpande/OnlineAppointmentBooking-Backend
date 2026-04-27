package com.medibook.provider.service.impl;

import com.medibook.provider.dto.ProviderRequestDto;
import com.medibook.provider.dto.ProviderResponseDto;
import com.medibook.provider.entity.Provider;
import com.medibook.provider.exception.DuplicateResourceException;
import com.medibook.provider.exception.ResourceNotFoundException;
import com.medibook.provider.repository.ProviderRepository;
import com.medibook.provider.service.ProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ProviderServiceImpl implements ProviderService {

    private final ProviderRepository providerRepository;

    public ProviderServiceImpl(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @Override
    public ProviderResponseDto registerProvider(ProviderRequestDto requestDto) {
        log.info("Register provider request received for userId: {}", requestDto.getUserId());

        providerRepository.findByUserId(requestDto.getUserId()).ifPresent(existing -> {
            log.warn("Provider already exists for userId={}", requestDto.getUserId());
            throw new DuplicateResourceException("Provider already exists for userId: " + requestDto.getUserId());
        });

        Provider provider = Provider.builder()
                .userId(requestDto.getUserId())
                .fullName(requestDto.getFullName())
                .specialization(requestDto.getSpecialization())
                .qualification(requestDto.getQualification())
                .experienceYears(requestDto.getExperienceYears())
                .bio(requestDto.getBio())
                .clinicName(requestDto.getClinicName())
                .clinicAddress(requestDto.getClinicAddress())
                .build();

        Provider savedProvider = providerRepository.save(provider);
        log.info("Provider registered successfully with providerId: {}", savedProvider.getProviderId());

        return mapToResponse(savedProvider);
    }

    @Override
    public ProviderResponseDto getProviderById(Long providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));
        return mapToResponse(provider);
    }

    @Override
    public ProviderResponseDto getProviderByUserId(Long userId) {
        Provider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with userId: " + userId));
        return mapToResponse(provider);
    }

    @Override
    public List<ProviderResponseDto> getAllProviders() {
        return providerRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<ProviderResponseDto> getProvidersBySpecialization(String specialization) {
        return providerRepository.findBySpecializationIgnoreCase(specialization)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public ProviderResponseDto updateProvider(Long providerId, ProviderRequestDto requestDto) {
        log.info("Update provider request received for providerId: {}", providerId);

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        provider.setSpecialization(requestDto.getSpecialization());
        provider.setQualification(requestDto.getQualification());
        provider.setExperienceYears(requestDto.getExperienceYears());
        provider.setBio(requestDto.getBio());
        provider.setClinicName(requestDto.getClinicName());
        provider.setClinicAddress(requestDto.getClinicAddress());

        Provider updatedProvider = providerRepository.save(provider);
        log.info("Provider updated successfully with providerId: {}", updatedProvider.getProviderId());

        return mapToResponse(updatedProvider);
    }

    @Override
    public List<ProviderResponseDto> searchProviders(String keyword) {
        return providerRepository
                .findByClinicNameContainingIgnoreCaseOrSpecializationContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public ProviderResponseDto verifyProvider(Long providerId, Boolean verified) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        provider.setIsVerified(verified);
        return mapToResponse(providerRepository.save(provider));
    }

    @Override
    public ProviderResponseDto updateAvailability(Long providerId, Boolean available) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        provider.setIsAvailable(available);
        return mapToResponse(providerRepository.save(provider));
    }

    @Override
    public ProviderResponseDto updateRating(Long providerId, Double avgRating) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        provider.setAvgRating(avgRating);
        return mapToResponse(providerRepository.save(provider));
    }

    private ProviderResponseDto mapToResponse(Provider provider) {
        return ProviderResponseDto.builder()
                .providerId(provider.getProviderId())
                .userId(provider.getUserId())
                .fullName(provider.getFullName())
                .specialization(provider.getSpecialization())
                .qualification(provider.getQualification())
                .experienceYears(provider.getExperienceYears())
                .bio(provider.getBio())
                .clinicName(provider.getClinicName())
                .clinicAddress(provider.getClinicAddress())
                .avgRating(provider.getAvgRating())
                .isVerified(provider.getIsVerified())
                .isAvailable(provider.getIsAvailable())
                .build();
    }
}