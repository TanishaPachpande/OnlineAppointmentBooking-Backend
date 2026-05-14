package com.medibook.provider.entity;

public enum VerificationStatus {
    PENDING,    // submitted, awaiting admin review
    APPROVED,   // admin approved
    REJECTED    // admin rejected (with note)
}
