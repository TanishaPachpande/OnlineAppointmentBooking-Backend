package com.medibook.review.service.impl;

import com.medibook.review.dto.*;
import com.medibook.review.entity.Review;
import com.medibook.review.exception.BusinessException;
import com.medibook.review.exception.ResourceNotFoundException;
import com.medibook.review.repository.ReviewRepository;
import com.medibook.review.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    public ReviewResponseDto addReview(ReviewRequestDto requestDto) {
        log.info("Adding review for appointmentId={}, providerId={}, patientId={}",
                requestDto.getAppointmentId(), requestDto.getProviderId(), requestDto.getPatientId());

        reviewRepository.findByAppointmentId(requestDto.getAppointmentId()).ifPresent(existing -> {
            log.warn("Review already exists for appointmentId={}", requestDto.getAppointmentId());
            throw new BusinessException("Review already exists for this appointment");
        });

        Review review = Review.builder()
                .appointmentId(requestDto.getAppointmentId())
                .providerId(requestDto.getProviderId())
                .patientId(requestDto.getPatientId())
                .starRating(requestDto.getStarRating())
                .comment(requestDto.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);

        log.info("Review saved successfully with reviewId={}", savedReview.getReviewId());

        return mapToResponse(savedReview);
    }

    @Override
    public ReviewResponseDto getReviewById(Long reviewId) {
        log.info("Fetching review by reviewId={}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.error("Review not found with id={}", reviewId);
                    return new ResourceNotFoundException("Review not found with id: " + reviewId);
                });

        return mapToResponse(review);
    }

    @Override
    public ReviewResponseDto getReviewByAppointmentId(Long appointmentId) {
        log.info("Fetching review by appointmentId={}", appointmentId);

        Review review = reviewRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> {
                    log.error("Review not found for appointmentId={}", appointmentId);
                    return new ResourceNotFoundException("Review not found for appointmentId: " + appointmentId);
                });

        return mapToResponse(review);
    }

    @Override
    public List<ReviewResponseDto> getReviewsByProvider(Long providerId) {
        log.info("Fetching reviews for providerId={}", providerId);

        return reviewRepository.findByProviderId(providerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<ReviewResponseDto> getReviewsByPatient(Long patientId) {
        log.info("Fetching reviews for patientId={}", patientId);

        return reviewRepository.findByPatientId(patientId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public ProviderRatingSummaryDto getProviderRatingSummary(Long providerId) {
        log.info("Calculating rating summary for providerId={}", providerId);

        List<Review> reviews = reviewRepository.findByProviderId(providerId);

        if (reviews.isEmpty()) {
            return ProviderRatingSummaryDto.builder()
                    .providerId(providerId)
                    .averageRating(0.0)
                    .totalReviews(0L)
                    .build();
        }

        double avg = reviews.stream()
                .mapToInt(Review::getStarRating)
                .average()
                .orElse(0.0);

        long totalReviews = reviewRepository.countByProviderId(providerId);

        return ProviderRatingSummaryDto.builder()
                .providerId(providerId)
                .averageRating(avg)
                .totalReviews(totalReviews)
                .build();
    }

    @Override
    public String deleteReview(Long reviewId) {
        log.info("Deleting reviewId={}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.error("Review not found with id={}", reviewId);
                    return new ResourceNotFoundException("Review not found with id: " + reviewId);
                });

        reviewRepository.delete(review);

        log.info("Review deleted successfully for reviewId={}", reviewId);
        return "Review deleted successfully";
    }

    private ReviewResponseDto mapToResponse(Review review) {
        return ReviewResponseDto.builder()
                .reviewId(review.getReviewId())
                .appointmentId(review.getAppointmentId())
                .providerId(review.getProviderId())
                .patientId(review.getPatientId())
                .starRating(review.getStarRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}