package com.medibook.provider.service;

import com.medibook.provider.dto.ProviderRequestDto;
import com.medibook.provider.dto.ProviderResponseDto;
import com.medibook.provider.dto.ProviderVerificationActionDto;
import com.medibook.provider.entity.Provider;
import com.medibook.provider.entity.VerificationStatus;
import com.medibook.provider.exception.DuplicateResourceException;
import com.medibook.provider.exception.ResourceNotFoundException;
import com.medibook.provider.repository.ProviderRepository;
import com.medibook.provider.service.impl.ProviderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderServiceImplTest {

    @Mock private ProviderRepository providerRepository;
    @InjectMocks private ProviderServiceImpl providerService;

    private Provider sampleProvider;
    private ProviderRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        sampleProvider = Provider.builder()
                .providerId(1L).userId(10L).fullName("Dr. Smith")
                .specialization("Cardiology").qualification("MBBS, MD").experienceYears(10)
                .bio("Heart specialist").clinicName("Heart Clinic").clinicAddress("123 Main St")
                .avgRating(0.0).isVerified(false).isAvailable(true)
                .verificationStatus(VerificationStatus.PENDING)
                .createdAt(LocalDateTime.now()).build();

        sampleRequest = ProviderRequestDto.builder()
                .userId(10L).fullName("Dr. Smith").specialization("Cardiology")
                .qualification("MBBS, MD").experienceYears(10).bio("Heart specialist")
                .clinicName("Heart Clinic").clinicAddress("123 Main St").build();
    }

    // ── registerProvider ───────────────────────────────────────────────────────

    @Test
    void registerProvider_success() {
        when(providerRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(providerRepository.save(any())).thenReturn(sampleProvider);

        ProviderResponseDto result = providerService.registerProvider(sampleRequest);

        assertThat(result.getProviderId()).isEqualTo(1L);
        assertThat(result.getSpecialization()).isEqualTo("Cardiology");
        verify(providerRepository).save(any());
    }

    @Test
    void registerProvider_withProfilePhoto_saved() {
        sampleRequest.setProfilePhotoUrl("http://example.com/photo.jpg");
        when(providerRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(providerRepository.save(any())).thenReturn(sampleProvider);

        ProviderResponseDto result = providerService.registerProvider(sampleRequest);
        assertThat(result).isNotNull();
        verify(providerRepository).save(any());
    }

    @Test
    void registerProvider_duplicateUserId_throws() {
        when(providerRepository.findByUserId(10L)).thenReturn(Optional.of(sampleProvider));

        assertThatThrownBy(() -> providerService.registerProvider(sampleRequest))
                .isInstanceOf(DuplicateResourceException.class).hasMessageContaining("already exists");
    }

    // ── getProviderById ────────────────────────────────────────────────────────

    @Test
    void getProviderById_found() {
        when(providerRepository.findById(1L)).thenReturn(Optional.of(sampleProvider));
        assertThat(providerService.getProviderById(1L).getFullName()).isEqualTo("Dr. Smith");
    }

    @Test
    void getProviderById_notFound_throws() {
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> providerService.getProviderById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getProviderByUserId ────────────────────────────────────────────────────

    @Test
    void getProviderByUserId_found() {
        when(providerRepository.findByUserId(10L)).thenReturn(Optional.of(sampleProvider));
        assertThat(providerService.getProviderByUserId(10L).getUserId()).isEqualTo(10L);
    }

    @Test
    void getProviderByUserId_notFound_throws() {
        when(providerRepository.findByUserId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> providerService.getProviderByUserId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getAllProviders ────────────────────────────────────────────────────────

    @Test
    void getAllProviders_returnsList() {
        when(providerRepository.findAll()).thenReturn(List.of(sampleProvider, sampleProvider));
        assertThat(providerService.getAllProviders()).hasSize(2);
    }

    // ── getProvidersBySpecialization ───────────────────────────────────────────

    @Test
    void getProvidersBySpecialization_returnsList() {
        when(providerRepository.findBySpecializationIgnoreCase("cardiology"))
                .thenReturn(List.of(sampleProvider));
        assertThat(providerService.getProvidersBySpecialization("cardiology")).hasSize(1);
    }

    // ── updateProvider ─────────────────────────────────────────────────────────

    @Test
    void updateProvider_success() {
        when(providerRepository.findById(1L)).thenReturn(Optional.of(sampleProvider));
        when(providerRepository.save(any())).thenReturn(sampleProvider);

        ProviderResponseDto result = providerService.updateProvider(1L, sampleRequest);
        assertThat(result).isNotNull();
        verify(providerRepository).save(any());
    }

    @Test
    void updateProvider_withNewProfilePhoto_updatesPhoto() {
        sampleRequest.setProfilePhotoUrl("http://example.com/new.jpg");
        when(providerRepository.findById(1L)).thenReturn(Optional.of(sampleProvider));
        when(providerRepository.save(any())).thenReturn(sampleProvider);

        ProviderResponseDto result = providerService.updateProvider(1L, sampleRequest);
        assertThat(result).isNotNull();
        verify(providerRepository).save(any());
    }

    @Test
    void updateProvider_withNewDocument_resetsToPending() {
        sampleRequest.setVerificationDocumentUrl("http://example.com/doc.pdf");
        when(providerRepository.findById(1L)).thenReturn(Optional.of(sampleProvider));
        when(providerRepository.save(any())).thenReturn(sampleProvider);

        providerService.updateProvider(1L, sampleRequest);

        // Verify the provider's status was reset to PENDING
        verify(providerRepository).save(argThat(p ->
                p.getVerificationStatus() == VerificationStatus.PENDING && !p.getIsVerified()));
    }

    @Test
    void updateProvider_notFound_throws() {
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> providerService.updateProvider(99L, sampleRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── searchProviders ────────────────────────────────────────────────────────

    @Test
    void searchProviders_returnsList() {
        when(providerRepository.findByClinicNameContainingIgnoreCaseOrSpecializationContainingIgnoreCase("Heart", "Heart"))
                .thenReturn(List.of(sampleProvider));
        assertThat(providerService.searchProviders("Heart")).hasSize(1);
    }

    // ── verifyProvider (legacy toggle) ────────────────────────────────────────

    @Test
    void verifyProvider_approves() {
        when(providerRepository.findById(1L)).thenReturn(Optional.of(sampleProvider));
        when(providerRepository.save(any())).thenReturn(sampleProvider);

        ProviderResponseDto result = providerService.verifyProvider(1L, true);
        assertThat(result).isNotNull();
        verify(providerRepository).save(argThat(p ->
                p.getIsVerified() && p.getVerificationStatus() == VerificationStatus.APPROVED));
    }

    @Test
    void verifyProvider_rejects() {
        when(providerRepository.findById(1L)).thenReturn(Optional.of(sampleProvider));
        when(providerRepository.save(any())).thenReturn(sampleProvider);

        providerService.verifyProvider(1L, false);

        verify(providerRepository).save(argThat(p ->
                !p.getIsVerified() && p.getVerificationStatus() == VerificationStatus.REJECTED));
    }

    // ── reviewProvider (admin decision) ───────────────────────────────────────

    @Test
    void reviewProvider_approved() {
        ProviderVerificationActionDto action = new ProviderVerificationActionDto(true, "Looks good");
        when(providerRepository.findById(1L)).thenReturn(Optional.of(sampleProvider));
        when(providerRepository.save(any())).thenReturn(sampleProvider);

        ProviderResponseDto result = providerService.reviewProvider(1L, action);
        assertThat(result).isNotNull();
        verify(providerRepository).save(argThat(p ->
                p.getIsVerified() && p.getVerificationStatus() == VerificationStatus.APPROVED));
    }

    @Test
    void reviewProvider_rejected_withNote() {
        ProviderVerificationActionDto action = new ProviderVerificationActionDto(false, "Docs incomplete");
        when(providerRepository.findById(1L)).thenReturn(Optional.of(sampleProvider));
        when(providerRepository.save(any())).thenReturn(sampleProvider);

        providerService.reviewProvider(1L, action);

        verify(providerRepository).save(argThat(p ->
                !p.getIsVerified()
                        && p.getVerificationStatus() == VerificationStatus.REJECTED
                        && "Docs incomplete".equals(p.getVerificationNote())));
    }

    @Test
    void reviewProvider_notFound_throws() {
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> providerService.reviewProvider(99L,
                new ProviderVerificationActionDto(true, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPendingProviders ────────────────────────────────────────────────────

    @Test
    void getPendingProviders_returnsPendingList() {
        when(providerRepository.findByVerificationStatus(VerificationStatus.PENDING))
                .thenReturn(List.of(sampleProvider));
        assertThat(providerService.getPendingProviders()).hasSize(1);
    }

    @Test
    void getPendingProviders_empty() {
        when(providerRepository.findByVerificationStatus(VerificationStatus.PENDING))
                .thenReturn(List.of());
        assertThat(providerService.getPendingProviders()).isEmpty();
    }

    // ── updateAvailability ─────────────────────────────────────────────────────

    @Test
    void updateAvailability_success() {
        when(providerRepository.findById(1L)).thenReturn(Optional.of(sampleProvider));
        when(providerRepository.save(any())).thenReturn(sampleProvider);

        ProviderResponseDto result = providerService.updateAvailability(1L, false);
        assertThat(result).isNotNull();
        verify(providerRepository).save(argThat(p -> !p.getIsAvailable()));
    }

    @Test
    void updateAvailability_notFound_throws() {
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> providerService.updateAvailability(99L, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateRating ───────────────────────────────────────────────────────────

    @Test
    void updateRating_success() {
        when(providerRepository.findById(1L)).thenReturn(Optional.of(sampleProvider));
        when(providerRepository.save(any())).thenReturn(sampleProvider);

        ProviderResponseDto result = providerService.updateRating(1L, 4.5);
        assertThat(result).isNotNull();
        verify(providerRepository).save(argThat(p -> p.getAvgRating() == 4.5));
    }

    @Test
    void updateRating_notFound_throws() {
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> providerService.updateRating(99L, 4.5))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
