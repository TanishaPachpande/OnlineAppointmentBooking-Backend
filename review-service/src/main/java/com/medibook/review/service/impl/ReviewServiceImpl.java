package com.medibook.review.service.impl;

import com.medibook.review.dto.*;
import com.medibook.review.entity.Review;
import com.medibook.review.exception.BusinessException;
import com.medibook.review.exception.ResourceNotFoundException;
import com.medibook.review.messaging.NotificationProducer;
import com.medibook.review.repository.ReviewRepository;
import com.medibook.review.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    // Demo email for the doctor — same as appointment-service
    private static final String DEMO_PROVIDER_EMAIL = "tanishapachpande86@gmail.com";

    private final ReviewRepository     reviewRepository;
    private final RestTemplate         restTemplate;
    private final NotificationProducer notificationProducer;

    @Value("${provider.service.url:http://localhost:8083}")
    private String providerServiceUrl;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             RestTemplate restTemplate,
                             NotificationProducer notificationProducer) {
        this.reviewRepository     = reviewRepository;
        this.restTemplate         = restTemplate;
        this.notificationProducer = notificationProducer;
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

        // Update provider's average rating in provider-service (REST)
        updateProviderAvgRating(requestDto.getProviderId());

        // Notify the doctor via RabbitMQ → notification-service
        notifyDoctor(savedReview);

        return mapToResponse(savedReview);
    }

    /**
     * Sends an email notification to the doctor informing them of the new rating.
     */
    private void notifyDoctor(Review review) {
        try {
            String subject = "New Rating Received – MediBook";
            String message = "Dear Doctor,\n\n" +
                    "A patient has rated your service.\n\n" +
                    "Rating Details:\n" +
                    "  Appointment ID : " + review.getAppointmentId() + "\n" +
                    "  Star Rating    : " + review.getStarRating() + " / 5\n" +
                    "  Comment        : " + (review.getComment() != null ? review.getComment() : "-") + "\n\n" +
                    "Thank you for providing excellent care.\n" +
                    "– The MediBook Team";

            NotificationMessage msg = NotificationMessage.builder()
                    .userId(review.getProviderId())
                    .recipient(DEMO_PROVIDER_EMAIL)
                    .type("EMAIL")
                    .subject(subject)
                    .message(message)
                    .build();

            notificationProducer.publishNotification(msg);
            log.info("Review notification published for providerId={}", review.getProviderId());
        } catch (Exception e) {
            // Non-critical — don't fail the review submission if notification fails
            log.warn("Failed to send review notification for providerId={}: {}",
                    review.getProviderId(), e.getMessage());
        }
    }

    /**
     * Recalculates the average rating for the given provider from all reviews
     * and pushes it to the provider-service via REST.
     */
    private void updateProviderAvgRating(Long providerId) {
        try {
            List<Review> reviews = reviewRepository.findByProviderId(providerId);
            double avg = reviews.stream()
                    .mapToInt(Review::getStarRating)
                    .average()
                    .orElse(0.0);

            String url = providerServiceUrl + "/providers/" + providerId + "/rating?avgRating=" + avg;
            restTemplate.put(url, null);
            log.info("Updated avgRating={} for providerId={}", avg, providerId);
        } catch (Exception e) {
            log.warn("Failed to update avgRating for providerId={}: {}", providerId, e.getMessage());
        }
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
        return reviewRepository.findByProviderId(providerId).stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<ReviewResponseDto> getReviewsByPatient(Long patientId) {
        log.info("Fetching reviews for patientId={}", patientId);
        return reviewRepository.findByPatientId(patientId).stream().map(this::mapToResponse).toList();
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

        double avg = reviews.stream().mapToInt(Review::getStarRating).average().orElse(0.0);
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
