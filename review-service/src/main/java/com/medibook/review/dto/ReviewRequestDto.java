package com.medibook.review.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequestDto {

    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;

    @NotNull(message = "Provider ID is required")
    private Long providerId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Star rating is required")
    @Min(value = 1, message = "Star rating must be at least 1")
    @Max(value = 5, message = "Star rating must be at most 5")
    private Integer starRating;

    @NotBlank(message = "Comment is required")
    @Size(min = 3, max = 1000, message = "Comment must be between 3 and 1000 characters")
    private String comment;
}