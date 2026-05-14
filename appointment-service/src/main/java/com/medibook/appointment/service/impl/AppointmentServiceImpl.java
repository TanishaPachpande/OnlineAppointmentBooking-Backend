package com.medibook.appointment.service.impl;

import com.medibook.appointment.client.ProviderClient;
import com.medibook.appointment.client.ScheduleClient;
import com.medibook.appointment.dto.*;
import com.medibook.appointment.entity.*;
import com.medibook.appointment.exception.BusinessException;
import com.medibook.appointment.exception.ResourceNotFoundException;
import com.medibook.appointment.messaging.NotificationProducer;
import com.medibook.appointment.repository.AppointmentRepository;
import com.medibook.appointment.service.AppointmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;   // ── ADDED ──
import java.util.List;

@Service
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private static final String DEMO_PATIENT_EMAIL = "tanishapachpande0072@gmail.com";
    private static final String DEMO_PROVIDER_EMAIL = "tanishapachpande86@gmail.com";

    private final AppointmentRepository appointmentRepository;
    private final ScheduleClient scheduleClient;
    private final NotificationProducer notificationProducer;
    private final ProviderClient providerClient;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  ScheduleClient scheduleClient,
                                  NotificationProducer notificationProducer,
                                  ProviderClient providerClient) {
        this.appointmentRepository = appointmentRepository;
        this.scheduleClient = scheduleClient;
        this.notificationProducer = notificationProducer;
        this.providerClient = providerClient;
    }

    @Override
    public AppointmentResponseDto bookAppointment(BookAppointmentRequestDto requestDto) {
        log.info("Booking appointment for patientId={}, providerId={}, slotId={}",
                requestDto.getPatientId(), requestDto.getProviderId(), requestDto.getSlotId());

        SlotResponseDto slot = scheduleClient.getSlotById(requestDto.getSlotId());

        if (slot == null) {
            log.error("Slot not found with id={}", requestDto.getSlotId());
            throw new ResourceNotFoundException("Slot not found with id: " + requestDto.getSlotId());
        }

        if (Boolean.TRUE.equals(slot.getIsBooked())) {
            log.warn("Selected slot is already booked. slotId={}", requestDto.getSlotId());
            throw new BusinessException("Selected slot is already booked");
        }

        if (Boolean.TRUE.equals(slot.getIsBlocked())) {
            log.warn("Selected slot is blocked. slotId={}", requestDto.getSlotId());
            throw new BusinessException("Selected slot is blocked");
        }

        // ── ADDED: Reject booking if the slot is in the past ──
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();

        if (slot.getDate().isBefore(today)) {
            log.warn("Attempted to book a past-date slot. slotId={}, slotDate={}", requestDto.getSlotId(), slot.getDate());
            throw new BusinessException("Cannot book a slot that is in the past.");
        }
        if (slot.getDate().equals(today) && !slot.getStartTime().isAfter(now)) {
            log.warn("Attempted to book an elapsed today-slot. slotId={}, startTime={}", requestDto.getSlotId(), slot.getStartTime());
            throw new BusinessException("Cannot book a slot whose start time has already passed.");
        }
        // ── END ADDED ──

        scheduleClient.bookSlot(requestDto.getSlotId());

        Appointment appointment = Appointment.builder()
                .patientId(requestDto.getPatientId())
                .providerId(requestDto.getProviderId())
                .slotId(requestDto.getSlotId())
                .serviceType(requestDto.getServiceType())
                .appointmentDate(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .notes(requestDto.getNotes())
                .modeOfConsultation(requestDto.getModeOfConsultation())
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Appointment booked successfully with appointmentId={}", savedAppointment.getAppointmentId());

        // Fetch provider details for the email
        String clinicName = "";
        String clinicAddress = "";
        String providerName = "your doctor";
        Long providerUserId = savedAppointment.getProviderId(); // fallback
        try {
            ProviderResponseDto provider = providerClient.getProviderById(savedAppointment.getProviderId());
            if (provider != null) {
                clinicName = provider.getClinicName() != null ? provider.getClinicName() : "";
                clinicAddress = provider.getClinicAddress() != null ? provider.getClinicAddress() : "";
                providerName = provider.getFullName() != null ? "Dr. " + provider.getFullName() : "your doctor";
                if (provider.getUserId() != null) providerUserId = provider.getUserId();
            }
        } catch (Exception e) {
            log.warn("Could not fetch provider details for email. providerId={}", savedAppointment.getProviderId());
        }

        publishEmailNotification(
                savedAppointment.getPatientId(),
                DEMO_PATIENT_EMAIL,
                "Appointment Booked Successfully – MediBook",
                "Dear Patient,\n\n" +
                        "Your appointment has been booked successfully.\n\n" +
                        "Appointment Details:\n" +
                        "  Doctor     : " + providerName + "\n" +
                        "  Date       : " + savedAppointment.getAppointmentDate() + "\n" +
                        "  Time       : " + savedAppointment.getStartTime() + "\n" +
                        "  Clinic     : " + clinicName + "\n" +
                        "  Location   : " + clinicAddress + "\n" +
                        "  Mode       : " + savedAppointment.getModeOfConsultation() + "\n\n" +
                        "Please arrive 10 minutes before your scheduled time.\n\n" +
                        "Thank you for choosing MediBook.\n" +
                        "– The MediBook Team"
        );

        publishEmailNotification(
                providerUserId,
                DEMO_PROVIDER_EMAIL,
                "New Appointment Booked",
                "A new appointment has been booked.\n" +
                        "Date: " + savedAppointment.getAppointmentDate() + "\n" +
                        "Time: " + savedAppointment.getStartTime() + " - " + savedAppointment.getEndTime() + "\n" +
                        "Service: " + savedAppointment.getServiceType() + "\n" +
                        "Mode: " + savedAppointment.getModeOfConsultation()
        );

        return mapToResponse(savedAppointment);
    }

    @Override
    public AppointmentResponseDto getById(Long appointmentId) {
        log.info("Fetching appointment by id={}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    log.error("Appointment not found with id={}", appointmentId);
                    return new ResourceNotFoundException("Appointment not found with id: " + appointmentId);
                });

        return mapToResponse(appointment);
    }

    @Override
    public List<AppointmentResponseDto> getByPatient(Long patientId) {
        log.info("Fetching appointments for patientId={}", patientId);

        return appointmentRepository.findByPatientId(patientId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AppointmentResponseDto> getByProvider(Long providerId) {
        log.info("Fetching appointments for providerId={}", providerId);

        return appointmentRepository.findByProviderId(providerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AppointmentResponseDto> getByProviderAndDate(Long providerId, LocalDate date) {
        log.info("Fetching appointments for providerId={} on date={}", providerId, date);

        return appointmentRepository.findByProviderIdAndAppointmentDate(providerId, date)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public AppointmentResponseDto cancelAppointment(Long appointmentId) {
        log.info("Cancelling appointmentId={}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    log.error("Appointment not found with id={}", appointmentId);
                    return new ResourceNotFoundException("Appointment not found with id: " + appointmentId);
                });

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            log.warn("Appointment already cancelled. appointmentId={}", appointmentId);
            throw new BusinessException("Appointment is already cancelled");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            log.warn("Completed appointment cannot be cancelled. appointmentId={}", appointmentId);
            throw new BusinessException("Completed appointment cannot be cancelled");
        }

        scheduleClient.unbookSlot(appointment.getSlotId());
        log.info("Slot {} successfully unbooked after cancellation of appointmentId={}", appointment.getSlotId(), appointmentId);

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);

        Long cancelProviderUserId = saved.getProviderId();
        try {
            ProviderResponseDto cp = providerClient.getProviderById(saved.getProviderId());
            if (cp != null && cp.getUserId() != null) cancelProviderUserId = cp.getUserId();
        } catch (Exception e) {
            log.warn("Could not fetch provider userId for cancel notification. providerId={}", saved.getProviderId());
        }

        publishEmailNotification(
                saved.getPatientId(),
                DEMO_PATIENT_EMAIL,
                "Appointment Cancelled & Refund Initiated – MediBook",
                "Dear Patient,\n\n" +
                        "Your appointment #" + saved.getAppointmentId() + " has been cancelled successfully.\n\n" +
                        "Appointment Details:\n" +
                        "  Date   : " + saved.getAppointmentDate() + "\n" +
                        "  Time   : " + saved.getStartTime() + "\n\n" +
                        "If you had made a payment, your refund has been initiated and will reflect within 5-7 business days.\n\n" +
                        "Thank you for using MediBook.\n" +
                        "– The MediBook Team"
        );

        publishEmailNotification(
                cancelProviderUserId,
                DEMO_PROVIDER_EMAIL,
                "Appointment Cancelled by Patient – MediBook",
                "Dear Doctor,\n\n" +
                        "Appointment #" + saved.getAppointmentId() +
                        " scheduled for " + saved.getAppointmentDate() +
                        " at " + saved.getStartTime() +
                        " has been cancelled by the patient.\n\n" +
                        "The time slot is now available for other patients.\n\n" +
                        "– The MediBook Team"
        );

        log.info("Appointment cancelled successfully for appointmentId={}", appointmentId);
        return mapToResponse(saved);
    }


    @Override
    public AppointmentResponseDto rescheduleAppointment(Long appointmentId, RescheduleAppointmentRequestDto requestDto) {
        log.info("Rescheduling appointmentId={} to newSlotId={}", appointmentId, requestDto.getNewSlotId());

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    log.error("Appointment not found with id={}", appointmentId);
                    return new ResourceNotFoundException("Appointment not found with id: " + appointmentId);
                });

        if (appointment.getStatus() == AppointmentStatus.CANCELLED ||
                appointment.getStatus() == AppointmentStatus.COMPLETED) {
            log.warn("Only scheduled appointments can be rescheduled. appointmentId={}", appointmentId);
            throw new BusinessException("Only scheduled appointments can be rescheduled");
        }

        SlotResponseDto newSlot = scheduleClient.getSlotById(requestDto.getNewSlotId());

        if (newSlot == null) {
            log.error("New slot not found with id={}", requestDto.getNewSlotId());
            throw new ResourceNotFoundException("New slot not found with id: " + requestDto.getNewSlotId());
        }

        if (Boolean.TRUE.equals(newSlot.getIsBooked()) || Boolean.TRUE.equals(newSlot.getIsBlocked())) {
            log.warn("New slot is not available. newSlotId={}", requestDto.getNewSlotId());
            throw new BusinessException("New slot is not available");
        }

        scheduleClient.bookSlot(requestDto.getNewSlotId());
        scheduleClient.unbookSlot(appointment.getSlotId());

        appointment.setSlotId(requestDto.getNewSlotId());
        appointment.setAppointmentDate(newSlot.getDate());
        appointment.setStartTime(newSlot.getStartTime());
        appointment.setEndTime(newSlot.getEndTime());

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment rescheduled successfully for appointmentId={}", appointmentId);

        Long rescheduleProviderUserId = saved.getProviderId();
        try {
            ProviderResponseDto rp = providerClient.getProviderById(saved.getProviderId());
            if (rp != null && rp.getUserId() != null) rescheduleProviderUserId = rp.getUserId();
        } catch (Exception e) {
            log.warn("Could not fetch provider userId for reschedule notification. providerId={}", saved.getProviderId());
        }

        publishEmailNotification(
                saved.getPatientId(),
                DEMO_PATIENT_EMAIL,
                "Appointment Rescheduled",
                "Your appointment has been rescheduled to "
                        + saved.getAppointmentDate() + " at " + saved.getStartTime()
        );

        publishEmailNotification(
                rescheduleProviderUserId,
                DEMO_PROVIDER_EMAIL,
                "Appointment Rescheduled",
                "Your appointment has been rescheduled to "
                        + saved.getAppointmentDate() + " at " + saved.getStartTime()
        );

        publishEmailNotification(
                saved.getProviderId(),
                DEMO_PROVIDER_EMAIL,
                "Appointment Rescheduled",
                "Appointment #" + saved.getAppointmentId() +
                        " has been rescheduled to " + saved.getAppointmentDate() +
                        " at " + saved.getStartTime() + "."
        );

        return mapToResponse(saved);
    }

    @Override
    public AppointmentResponseDto completeAppointment(Long appointmentId) {
        log.info("Completing appointmentId={}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    log.error("Appointment not found with id={}", appointmentId);
                    return new ResourceNotFoundException("Appointment not found with id: " + appointmentId);
                });

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            log.warn("Only scheduled appointments can be completed. appointmentId={}", appointmentId);
            throw new BusinessException("Only scheduled appointments can be completed");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment completed successfully for appointmentId={}", appointmentId);
        return mapToResponse(saved);
    }

    @Override
    public AppointmentResponseDto updateStatus(Long appointmentId, AppointmentStatus status) {
        log.info("Updating appointmentId={} to status={}", appointmentId, status);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    log.error("Appointment not found with id={}", appointmentId);
                    return new ResourceNotFoundException("Appointment not found with id: " + appointmentId);
                });

        appointment.setStatus(status);
        Appointment saved = appointmentRepository.save(appointment);

        log.info("Appointment status updated to {} for appointmentId={}", status, appointmentId);
        return mapToResponse(saved);
    }

    @Override
    public List<AppointmentResponseDto> getUpcomingByPatient(Long patientId) {
        log.info("Fetching upcoming appointments for patientId={}", patientId);

        return appointmentRepository.findByPatientIdAndStatus(patientId, AppointmentStatus.SCHEDULED)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public Long getAppointmentCount(Long providerId) {
        log.info("Fetching appointment count for providerId={}", providerId);
        return appointmentRepository.countByProviderId(providerId);
    }

    @Override
    public List<AppointmentResponseDto> getAllAppointments() {
        log.info("Fetching all appointments (admin)");
        return appointmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void publishEmailNotification(Long userId, String recipient, String subject, String message) {
        NotificationEventDto eventDto = NotificationEventDto.builder()
                .userId(userId)
                .recipient(recipient)
                .type("EMAIL")
                .subject(subject)
                .message(message)
                .build();

        notificationProducer.publishNotification(eventDto);
        log.info("Notification event published for userId={} with subject={}", userId, subject);
    }

    private AppointmentResponseDto mapToResponse(Appointment appointment) {
        return AppointmentResponseDto.builder()
                .appointmentId(appointment.getAppointmentId())
                .patientId(appointment.getPatientId())
                .providerId(appointment.getProviderId())
                .slotId(appointment.getSlotId())
                .serviceType(appointment.getServiceType())
                .appointmentDate(appointment.getAppointmentDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus())
                .notes(appointment.getNotes())
                .modeOfConsultation(appointment.getModeOfConsultation())
                .build();
    }
}
