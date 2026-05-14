package com.medibook.review.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDto {

    private Long reviewId;
    private Long appointmentId;
    private Long providerId;
    private Long patientId;
    private Integer starRating;
    private String comment;
    private LocalDateTime createdAt;
}