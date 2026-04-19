package com.medibook.appointment.repository;

import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByProviderId(Long providerId);

    List<Appointment> findBySlotId(Long slotId);

    List<Appointment> findByStatus(AppointmentStatus status);

    List<Appointment> findByProviderIdAndAppointmentDate(Long providerId, LocalDate appointmentDate);

    List<Appointment> findByPatientIdAndStatus(Long patientId, AppointmentStatus status);

    Long countByProviderId(Long providerId);
}