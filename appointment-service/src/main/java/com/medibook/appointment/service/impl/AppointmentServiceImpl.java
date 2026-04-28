package com.medibook.appointment.service.impl;

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

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private static final String DEMO_PATIENT_EMAIL = "tanishapachpande0072@gmail.com";
    private static final String DEMO_PROVIDER_EMAIL = "tanishapachpande86@gmail.com";

    private final AppointmentRepository appointmentRepository;
    private final ScheduleClient scheduleClient;
    private final NotificationProducer notificationProducer;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  ScheduleClient scheduleClient,
                                  NotificationProducer notificationProducer) {
        this.appointmentRepository = appointmentRepository;
        this.scheduleClient = scheduleClient;
        this.notificationProducer = notificationProducer;
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

        publishEmailNotification(
                savedAppointment.getPatientId(),
                DEMO_PATIENT_EMAIL,
                "Appointment Booked",
                "Your appointment has been booked successfully for "
                        + savedAppointment.getAppointmentDate() + " at " + savedAppointment.getStartTime()
        );

        publishEmailNotification(
                savedAppointment.getProviderId(),
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

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);


        scheduleClient.unbookSlot(appointment.getSlotId());
        publishEmailNotification(
                saved.getPatientId(),
                DEMO_PATIENT_EMAIL,
                "Appointment Cancelled",
                "Your appointment with id " + saved.getAppointmentId() + " has been cancelled."
        );

        publishEmailNotification(
                saved.getProviderId(),
                DEMO_PROVIDER_EMAIL,
                "Appointment Cancelled",
                "Appointment #" + saved.getAppointmentId() +
                        " scheduled for " + saved.getAppointmentDate() +
                        " at " + saved.getStartTime() +
                        " has been cancelled by the patient."
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
        scheduleClient.unblockSlot(appointment.getSlotId());

        appointment.setSlotId(requestDto.getNewSlotId());
        appointment.setAppointmentDate(newSlot.getDate());
        appointment.setStartTime(newSlot.getStartTime());
        appointment.setEndTime(newSlot.getEndTime());

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment rescheduled successfully for appointmentId={}", appointmentId);

        publishEmailNotification(
                saved.getPatientId(),
                DEMO_PATIENT_EMAIL,
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
        scheduleClient.unblockSlot(appointment.getSlotId());
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