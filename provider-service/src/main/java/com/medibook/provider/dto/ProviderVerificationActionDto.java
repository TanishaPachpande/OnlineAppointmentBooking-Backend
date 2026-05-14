package com.medibook.provider.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderVerificationActionDto {

    /** true = approve, false = reject */
    @NotNull(message = "Approval decision is required")
    private Boolean approved;

    /** Optional note from admin (e.g. reason for rejection) */
    private String note;
}
