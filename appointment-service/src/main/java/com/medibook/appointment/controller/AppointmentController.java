package com.medibook.appointment.controller;

import com.medibook.appointment.dto.*;
import com.medibook.appointment.entity.AppointmentStatus;
import com.medibook.appointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/appointments")
@Tag(name = "Appointment Controller", description = "APIs for appointment booking and lifecycle")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponseDto> bookAppointment(
            @Valid @RequestBody BookAppointmentRequestDto requestDto) {
        log.info("API CALL: Book appointment for patientId={}", requestDto.getPatientId());
        return ResponseEntity.ok(appointmentService.bookAppointment(requestDto));
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentResponseDto> getById(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(appointmentService.getById(appointmentId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AppointmentResponseDto>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(appointmentService.getByPatient(patientId));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<AppointmentResponseDto>> getByProvider(@PathVariable Long providerId) {
        return ResponseEntity.ok(appointmentService.getByProvider(providerId));
    }

    @GetMapping("/provider/{providerId}/date/{date}")
    public ResponseEntity<List<AppointmentResponseDto>> getByProviderAndDate(@PathVariable Long providerId,
                                                                             @PathVariable LocalDate date) {
        return ResponseEntity.ok(appointmentService.getByProviderAndDate(providerId, date));
    }

    @PutMapping("/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponseDto> cancelAppointment(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(appointmentId));
    }

    @PutMapping("/{appointmentId}/reschedule")
    public ResponseEntity<AppointmentResponseDto> rescheduleAppointment(
            @PathVariable Long appointmentId,
            @Valid @RequestBody RescheduleAppointmentRequestDto requestDto) {
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(appointmentId, requestDto));
    }

    @PutMapping("/{appointmentId}/complete")
    public ResponseEntity<AppointmentResponseDto> completeAppointment(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(appointmentService.completeAppointment(appointmentId));
    }

    @PutMapping("/{appointmentId}/status")
    public ResponseEntity<AppointmentResponseDto> updateStatus(@PathVariable Long appointmentId,
                                                               @RequestParam AppointmentStatus status) {
        return ResponseEntity.ok(appointmentService.updateStatus(appointmentId, status));
    }

    @GetMapping("/patient/{patientId}/upcoming")
    public ResponseEntity<List<AppointmentResponseDto>> getUpcomingByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(appointmentService.getUpcomingByPatient(patientId));
    }

    @GetMapping("/provider/{providerId}/count")
    public ResponseEntity<Long> getAppointmentCount(@PathVariable Long providerId) {
        return ResponseEntity.ok(appointmentService.getAppointmentCount(providerId));
    }
}