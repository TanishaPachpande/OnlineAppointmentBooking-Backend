package com.medibook.provider.dto;

import com.medibook.provider.entity.VerificationStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderResponseDto {

    private Long providerId;
    private Long userId;
    private String fullName;
    private String specialization;
    private String qualification;
    private Integer experienceYears;
    private String bio;
    private String clinicName;
    private String clinicAddress;
    private Double avgRating;
    private Boolean isVerified;
    private Boolean isAvailable;

    // ── Verification workflow fields ──
    private String verificationDocumentUrl;
    private String verificationNote;
    private VerificationStatus verificationStatus;

    // ── NEW: Profile photo URL  ────────────────────────────────────
    /** Cloudinary URL of the provider's profile photo. Null if not uploaded. */
    private String profilePhotoUrl;
    // ─────────────────────────────────────────────────────────────────────────
}