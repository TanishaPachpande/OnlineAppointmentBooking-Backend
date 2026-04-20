package com.medibook.review.service;

import com.medibook.review.dto.*;

import java.util.List;

public interface ReviewService {

    ReviewResponseDto addReview(ReviewRequestDto requestDto);

    ReviewResponseDto getReviewById(Long reviewId);

    ReviewResponseDto getReviewByAppointmentId(Long appointmentId);

    List<ReviewResponseDto> getReviewsByProvider(Long providerId);

    List<ReviewResponseDto> getReviewsByPatient(Long patientId);

    ProviderRatingSummaryDto getProviderRatingSummary(Long providerId);

    String deleteReview(Long reviewId);
}