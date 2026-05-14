package com.medibook.review.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderRatingSummaryDto {

    private Long providerId;
    private Double averageRating;
    private Long totalReviews;
}