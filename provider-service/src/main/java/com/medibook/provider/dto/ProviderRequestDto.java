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

    /**
     * URL of the credential document uploaded by the provider.
     * E.g. medical registration certificate, degree scan uploaded to cloud storage.
     * Required for new registrations so admin can verify credentials.
     */
    @NotBlank(message = "Verification document URL is required")
    private String verificationDocumentUrl;

    // ── NEW: Profile photo URL (optional) ────────────────────────────────────
    /** Cloudinary URL of the provider's profile photo. Optional — may be null or blank. */
    private String profilePhotoUrl;
    // ─────────────────────────────────────────────────────────────────────────
}