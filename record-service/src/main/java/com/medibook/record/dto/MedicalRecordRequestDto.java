package com.medibook.record.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecordRequestDto {

    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Provider ID is required")
    private Long providerId;

    @NotNull(message = "Visit date is required")
    private LocalDate visitDate;

    @NotBlank(message = "Diagnosis is required")
    @Size(min = 3, max = 1000, message = "Diagnosis must be between 3 and 1000 characters")
    private String diagnosis;

    @NotBlank(message = "Prescription is required")
    @Size(min = 3, max = 2000, message = "Prescription must be between 3 and 2000 characters")
    private String prescription;

    private String labTests;
    private String followUpNotes;
    private String allergies;
    private String vitals;
}