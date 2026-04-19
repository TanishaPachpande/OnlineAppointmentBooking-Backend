package com.medibook.appointment.dto;

import com.medibook.appointment.entity.ConsultationMode;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookAppointmentRequestDto {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Provider ID is required")
    private Long providerId;

    @NotNull(message = "Slot ID is required")
    private Long slotId;

    @NotBlank(message = "Service type is required")
    private String serviceType;

    private String notes;

    @NotNull(message = "Consultation mode is required")
    private ConsultationMode modeOfConsultation;
}