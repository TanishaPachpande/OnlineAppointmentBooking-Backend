package com.medibook.appointment.service;

import com.medibook.appointment.dto.*;
import com.medibook.appointment.entity.AppointmentStatus;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    AppointmentResponseDto bookAppointment(BookAppointmentRequestDto requestDto);

    AppointmentResponseDto getById(Long appointmentId);

    List<AppointmentResponseDto> getByPatient(Long patientId);

    List<AppointmentResponseDto> getByProvider(Long providerId);

    List<AppointmentResponseDto> getByProviderAndDate(Long providerId, LocalDate date);

    AppointmentResponseDto cancelAppointment(Long appointmentId);

    AppointmentResponseDto rescheduleAppointment(Long appointmentId, RescheduleAppointmentRequestDto requestDto);

    AppointmentResponseDto completeAppointment(Long appointmentId);

    AppointmentResponseDto updateStatus(Long appointmentId, AppointmentStatus status);

    List<AppointmentResponseDto> getUpcomingByPatient(Long patientId);

    Long getAppointmentCount(Long providerId);

    List<AppointmentResponseDto> getAllAppointments();
}