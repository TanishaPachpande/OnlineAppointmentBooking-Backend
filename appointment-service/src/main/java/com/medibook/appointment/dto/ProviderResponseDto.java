package com.medibook.appointment.dto;

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
    private String clinicName;
    private String clinicAddress;
}
