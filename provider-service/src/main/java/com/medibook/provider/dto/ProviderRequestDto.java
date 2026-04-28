package com.medibook.provider.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderRequestDto {

    @NotNull(message = "User ID is required")
    private Long userId;

    private String fullName;

    @NotBlank(message = "Specialization is required")
    private String specialization;

    @NotBlank(message = "Qualification is required")
    private String qualification;

    @NotNull(message = "Experience is required")
    @Min(value = 0, message = "Experience cannot be negative")
    private Integer experienceYears;

    @NotBlank(message = "Bio is required")
    private String bio;

    @NotBlank(message = "Clinic name is required")
    private String clinicName;

    @NotBlank(message = "Clinic address is required")
    private String clinicAddress;

}