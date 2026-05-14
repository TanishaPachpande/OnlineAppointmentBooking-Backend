package com.medibook.record.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecordResponseDto {

    private Long recordId;
    private Long appointmentId;
    private Long patientId;
    private Long providerId;
    private LocalDate visitDate;
    private String diagnosis;
    private String prescription;
    private String labTests;
    private String followUpNotes;
    private String allergies;
    private String vitals;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}