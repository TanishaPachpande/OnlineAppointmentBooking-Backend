package com.medibook.review.service;

import com.medibook.review.dto.ProviderRatingSummaryDto;
import com.medibook.review.dto.ReviewRequestDto;
import com.medibook.review.dto.ReviewResponseDto;
import com.medibook.review.entity.Review;
import com.medibook.review.exception.BusinessException;
import com.medibook.review.exception.ResourceNotFoundException;
import com.medibook.review.messaging.NotificationProducer;
import com.medibook.review.repository.ReviewRepository;
import com.medibook.review.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private RestTemplate restTemplate;
    @Mock private NotificationProducer notificationProducer;

    @InjectMocks private ReviewServiceImpl reviewService;

    private Review sampleReview;

    @BeforeEach
    void setUp() {
        sampleReview = Review.builder()
                .reviewId(1L).appointmentId(10L).providerId(20L).patientId(30L)
                .starRating(4).comment("Great service").createdAt(LocalDateTime.now()).build();
    }

    @Test
    void addReview_success() {
        ReviewRequestDto req = ReviewRequestDto.builder()
                .appointmentId(10L).providerId(20L).patientId(30L).starRating(4).comment("Great service").build();
        when(reviewRepository.findByAppointmentId(10L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any())).thenReturn(sampleReview);
        when(reviewRepository.findByProviderId(20L)).thenReturn(List.of(sampleReview));
        doNothing().when(notificationProducer).publishNotification(any());

        ReviewResponseDto result = reviewService.addReview(req);

        assertThat(result.getAppointmentId()).isEqualTo(10L);
        assertThat(result.getStarRating()).isEqualTo(4);
        verify(reviewRepository).save(any());
    }

    @Test
    void addReview_duplicateAppointment_throws() {
        ReviewRequestDto req = ReviewRequestDto.builder()
                .appointmentId(10L).providerId(20L).patientId(30L).starRating(4).comment("Good").build();
        when(reviewRepository.findByAppointmentId(10L)).thenReturn(Optional.of(sampleReview));

        assertThatThrownBy(() -> reviewService.addReview(req))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already exists");
    }

    @Test
    void addReview_notificationFailureDoesNotAbortSave() {
        ReviewRequestDto req = ReviewRequestDto.builder()
                .appointmentId(10L).providerId(20L).patientId(30L).starRating(5).comment("Excellent").build();
        when(reviewRepository.findByAppointmentId(10L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any())).thenReturn(sampleReview);
        when(reviewRepository.findByProviderId(20L)).thenReturn(List.of(sampleReview));
        doThrow(new RuntimeException("RabbitMQ down")).when(notificationProducer).publishNotification(any());

        assertThat(reviewService.addReview(req)).isNotNull();
    }

    @Test
    void getReviewById_found() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview));
        assertThat(reviewService.getReviewById(1L).getComment()).isEqualTo("Great service");
    }

    @Test
    void getReviewById_notFound_throws() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reviewService.getReviewById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getReviewByAppointmentId_found() {
        when(reviewRepository.findByAppointmentId(10L)).thenReturn(Optional.of(sampleReview));
        assertThat(reviewService.getReviewByAppointmentId(10L).getAppointmentId()).isEqualTo(10L);
    }

    @Test
    void getReviewByAppointmentId_notFound_throws() {
        when(reviewRepository.findByAppointmentId(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reviewService.getReviewByAppointmentId(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getReviewsByProvider_returnsList() {
        when(reviewRepository.findByProviderId(20L)).thenReturn(List.of(sampleReview, sampleReview));
        assertThat(reviewService.getReviewsByProvider(20L)).hasSize(2);
    }

    @Test
    void getReviewsByPatient_returnsList() {
        when(reviewRepository.findByPatientId(30L)).thenReturn(List.of(sampleReview));
        assertThat(reviewService.getReviewsByPatient(30L)).hasSize(1);
    }

    @Test
    void getProviderRatingSummary_calculatesAverage() {
        Review r2 = Review.builder().providerId(20L).starRating(2).createdAt(LocalDateTime.now()).build();
        when(reviewRepository.findByProviderId(20L)).thenReturn(List.of(sampleReview, r2));
        when(reviewRepository.countByProviderId(20L)).thenReturn(2L);

        ProviderRatingSummaryDto summary = reviewService.getProviderRatingSummary(20L);
        assertThat(summary.getAverageRating()).isEqualTo(3.0);
        assertThat(summary.getTotalReviews()).isEqualTo(2L);
    }

    @Test
    void getProviderRatingSummary_noReviews_returnsZero() {
        when(reviewRepository.findByProviderId(99L)).thenReturn(List.of());
        ProviderRatingSummaryDto summary = reviewService.getProviderRatingSummary(99L);
        assertThat(summary.getAverageRating()).isEqualTo(0.0);
        assertThat(summary.getTotalReviews()).isEqualTo(0L);
    }

    @Test
    void deleteReview_success() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview));
        doNothing().when(reviewRepository).delete(sampleReview);
        assertThat(reviewService.deleteReview(1L)).contains("deleted");
        verify(reviewRepository).delete(sampleReview);
    }

    @Test
    void deleteReview_notFound_throws() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reviewService.deleteReview(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
