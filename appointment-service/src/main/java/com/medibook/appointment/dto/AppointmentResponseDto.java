package com.medibook.appointment.dto;

import com.medibook.appointment.entity.AppointmentStatus;
import com.medibook.appointment.entity.ConsultationMode;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponseDto {

    private Long appointmentId;
    private Long patientId;
    private Long providerId;
    private Long slotId;
    private String serviceType;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private AppointmentStatus status;
    private String notes;
    private ConsultationMode modeOfConsultation;
}