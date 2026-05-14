package com.medibook.provider.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long providerId;

    @Column(length = 150)
    private String fullName;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String specialization;

    @Column(nullable = false, length = 150)
    private String qualification;

    @Column(nullable = false)
    private Integer experienceYears;

    @Column(length = 1000)
    private String bio;

    @Column(nullable = false, length = 150)
    private String clinicName;

    @Column(nullable = false, length = 255)
    private String clinicAddress;

    @Column(nullable = false)
    private Double avgRating;

    @Column(nullable = false)
    private Boolean isVerified;

    @Column(nullable = false)
    private Boolean isAvailable;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // ── Verification document & status ────────────────────────────────────────
    /** URL/path of the credential document uploaded by the provider (e.g. medical licence, degree). */
    @Column(length = 500)
    private String verificationDocumentUrl;

    /** Admin-readable note shown when approving or rejecting a provider. */
    @Column(length = 500)
    private String verificationNote;

    /** Tracks where the provider is in the admin approval workflow. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus verificationStatus;
    // ──────────────────────────────────────────────────────────────────────────

    // ── NEW: Profile photo ────────────────────────────────────────────────────
    /** Cloudinary URL of the provider's profile photo. Optional — may be null. */
    @Column(length = 500)
    private String profilePhotoUrl;
    // ──────────────────────────────────────────────────────────────────────────

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.avgRating == null) this.avgRating = 0.0;
        if (this.isVerified == null) this.isVerified = false;
        if (this.isAvailable == null) this.isAvailable = true;
        if (this.verificationStatus == null) this.verificationStatus = VerificationStatus.PENDING;
    }
}