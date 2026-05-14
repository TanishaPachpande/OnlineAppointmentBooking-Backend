package com.medibook.provider.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.provider.dto.*;
import com.medibook.provider.entity.VerificationStatus;
import com.medibook.provider.service.ProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProviderControllerTest {

    @Mock private ProviderService providerService;
    @InjectMocks private ProviderController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ProviderResponseDto sampleResponse;
    private ProviderRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        sampleResponse = ProviderResponseDto.builder()
                .providerId(1L).userId(10L).fullName("Dr. Alice Smith")
                .specialization("Cardiology").qualification("MBBS, MD")
                .experienceYears(10).bio("Experienced cardiologist")
                .clinicName("City Heart Clinic").clinicAddress("123 Main St")
                .avgRating(4.5).isVerified(true).isAvailable(true)
                .verificationStatus(VerificationStatus.APPROVED)
                .build();

        // Include ALL @NotBlank required fields
        sampleRequest = ProviderRequestDto.builder()
                .userId(10L)
                .fullName("Dr. Alice Smith")
                .specialization("Cardiology")
                .qualification("MBBS, MD")
                .experienceYears(10)
                .bio("Experienced cardiologist")
                .clinicName("City Heart Clinic")
                .clinicAddress("123 Main St")
                .verificationDocumentUrl("https://docs.example.com/cert.pdf")
                .build();
    }

    @Test
    void registerProvider_returns200() throws Exception {
        when(providerService.registerProvider(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value(1L))
                .andExpect(jsonPath("$.fullName").value("Dr. Alice Smith"));

        verify(providerService).registerProvider(any());
    }

    @Test
    void getProviderById_returns200() throws Exception {
        when(providerService.getProviderById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/providers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialization").value("Cardiology"));
    }

    @Test
    void getProviderByUserId_returns200() throws Exception {
        when(providerService.getProviderByUserId(10L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/providers/user/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10L));
    }

    @Test
    void getAllProviders_returns200() throws Exception {
        when(providerService.getAllProviders()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getBySpecialization_returns200() throws Exception {
        when(providerService.getProvidersBySpecialization("Cardiology")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/providers/specialization/Cardiology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void searchProviders_returns200() throws Exception {
        when(providerService.searchProviders("heart")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/providers/search").param("keyword", "heart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getPendingProviders_returns200() throws Exception {
        when(providerService.getPendingProviders()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/providers/pending-verification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void reviewProvider_approved_returns200() throws Exception {
        ProviderVerificationActionDto action = ProviderVerificationActionDto.builder()
                .approved(true).note("Documents verified").build();
        when(providerService.reviewProvider(eq(1L), any())).thenReturn(sampleResponse);

        mockMvc.perform(put("/providers/1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVerified").value(true));
    }

    @Test
    void verifyProvider_legacy_returns200() throws Exception {
        when(providerService.verifyProvider(1L, true)).thenReturn(sampleResponse);

        mockMvc.perform(put("/providers/1/verify").param("verified", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void updateAvailability_returns200() throws Exception {
        when(providerService.updateAvailability(1L, false)).thenReturn(sampleResponse);

        mockMvc.perform(put("/providers/1/availability").param("available", "false"))
                .andExpect(status().isOk());
    }

    @Test
    void updateRating_returns200() throws Exception {
        when(providerService.updateRating(1L, 4.8)).thenReturn(sampleResponse);

        mockMvc.perform(put("/providers/1/rating").param("avgRating", "4.8"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProvider_returns200() throws Exception {
        when(providerService.updateProvider(eq(1L), any())).thenReturn(sampleResponse);

        mockMvc.perform(put("/providers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value(1L));
    }

    @Test
    void getAllProviders_emptyList_returns200() throws Exception {
        when(providerService.getAllProviders()).thenReturn(List.of());

        mockMvc.perform(get("/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}