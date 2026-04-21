package com.medibook.review.service;

import com.medibook.review.dto.ProviderRatingSummaryDto;
import com.medibook.review.dto.ReviewRequestDto;
import com.medibook.review.dto.ReviewResponseDto;
import com.medibook.review.entity.Review;
import com.medibook.review.exception.BusinessException;
import com.medibook.review.repository.ReviewRepository;
import com.medibook.review.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void addReview_ShouldAddSuccessfully() {
        ReviewRequestDto requestDto = ReviewRequestDto.builder()
                .appointmentId(1L)
                .providerId(1L)
                .patientId(101L)
                .starRating(5)
                .comment("Excellent consultation")
                .build();

        Review savedReview = Review.builder()
                .reviewId(1L)
                .appointmentId(1L)
                .providerId(1L)
                .patientId(101L)
                .starRating(5)
                .comment("Excellent consultation")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        ReviewResponseDto response = reviewService.addReview(requestDto);

        assertNotNull(response);
        assertEquals(5, response.getStarRating());
        assertEquals("Excellent consultation", response.getComment());
    }

    @Test
    void addReview_ShouldThrowException_WhenAlreadyExistsForAppointment() {
        Review existingReview = Review.builder()
                .reviewId(1L)
                .appointmentId(1L)
                .build();

        ReviewRequestDto requestDto = ReviewRequestDto.builder()
                .appointmentId(1L)
                .providerId(1L)
                .patientId(101L)
                .starRating(5)
                .comment("Excellent consultation")
                .build();

        when(reviewRepository.findByAppointmentId(1L)).thenReturn(Optional.of(existingReview));

        assertThrows(BusinessException.class, () -> reviewService.addReview(requestDto));
    }

    @Test
    void getProviderRatingSummary_ShouldCalculateAverageCorrectly() {
        Review r1 = Review.builder().providerId(1L).starRating(5).build();
        Review r2 = Review.builder().providerId(1L).starRating(4).build();

        when(reviewRepository.findByProviderId(1L)).thenReturn(List.of(r1, r2));
        when(reviewRepository.countByProviderId(1L)).thenReturn(2L);

        ProviderRatingSummaryDto summary = reviewService.getProviderRatingSummary(1L);

        assertEquals(1L, summary.getProviderId());
        assertEquals(4.5, summary.getAverageRating());
        assertEquals(2L, summary.getTotalReviews());
    }
}