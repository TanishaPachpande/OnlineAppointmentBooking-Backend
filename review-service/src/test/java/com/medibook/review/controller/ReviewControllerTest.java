package com.medibook.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medibook.review.dto.*;
import com.medibook.review.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock private ReviewService reviewService;
    @InjectMocks private ReviewController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ReviewResponseDto sampleResponse;
    private ReviewRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleResponse = ReviewResponseDto.builder()
                .reviewId(1L).appointmentId(10L).providerId(20L).patientId(30L)
                .starRating(4).comment("Great service").createdAt(LocalDateTime.now())
                .build();

        sampleRequest = ReviewRequestDto.builder()
                .appointmentId(10L).providerId(20L).patientId(30L)
                .starRating(4).comment("Great service")
                .build();
    }

    @Test
    void addReview_returns200() throws Exception {
        when(reviewService.addReview(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(1L))
                .andExpect(jsonPath("$.starRating").value(4));

        verify(reviewService).addReview(any());
    }

    @Test
    void getReviewById_returns200() throws Exception {
        when(reviewService.getReviewById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("Great service"));
    }

    @Test
    void getReviewByAppointmentId_returns200() throws Exception {
        when(reviewService.getReviewByAppointmentId(10L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/reviews/appointment/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(10L));
    }

    @Test
    void getReviewsByProvider_returns200() throws Exception {
        when(reviewService.getReviewsByProvider(20L)).thenReturn(List.of(sampleResponse, sampleResponse));

        mockMvc.perform(get("/reviews/provider/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getReviewsByPatient_returns200() throws Exception {
        when(reviewService.getReviewsByPatient(30L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/reviews/patient/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getProviderRatingSummary_returns200() throws Exception {
        ProviderRatingSummaryDto summary = ProviderRatingSummaryDto.builder()
                .providerId(20L).averageRating(4.2).totalReviews(5L).build();
        when(reviewService.getProviderRatingSummary(20L)).thenReturn(summary);

        mockMvc.perform(get("/reviews/provider/20/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.2))
                .andExpect(jsonPath("$.totalReviews").value(5L));
    }

    @Test
    void deleteReview_returns200() throws Exception {
        when(reviewService.deleteReview(1L)).thenReturn("Review deleted successfully");

        mockMvc.perform(delete("/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Review deleted successfully"));
    }

    @Test
    void getReviewsByProvider_emptyList_returns200() throws Exception {
        when(reviewService.getReviewsByProvider(99L)).thenReturn(List.of());

        mockMvc.perform(get("/reviews/provider/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
