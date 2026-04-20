package com.medibook.review.controller;

import com.medibook.review.dto.*;
import com.medibook.review.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/reviews")
@Tag(name = "Review Controller", description = "APIs for review and rating management")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponseDto> addReview(@Valid @RequestBody ReviewRequestDto requestDto) {
        log.info("API CALL: Add review for appointmentId={}", requestDto.getAppointmentId());
        return ResponseEntity.ok(reviewService.addReview(requestDto));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDto> getReviewById(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.getReviewById(reviewId));
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<ReviewResponseDto> getReviewByAppointmentId(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(reviewService.getReviewByAppointmentId(appointmentId));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByProvider(@PathVariable Long providerId) {
        return ResponseEntity.ok(reviewService.getReviewsByProvider(providerId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(reviewService.getReviewsByPatient(patientId));
    }

    @GetMapping("/provider/{providerId}/summary")
    public ResponseEntity<ProviderRatingSummaryDto> getProviderRatingSummary(@PathVariable Long providerId) {
        return ResponseEntity.ok(reviewService.getProviderRatingSummary(providerId));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponseDto> deleteReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(
                ApiResponseDto.builder()
                        .message(reviewService.deleteReview(reviewId))
                        .build()
        );
    }
}