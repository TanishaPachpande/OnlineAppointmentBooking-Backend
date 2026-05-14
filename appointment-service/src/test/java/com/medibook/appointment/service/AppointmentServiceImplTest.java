package com.medibook.appointment.service;

import com.medibook.appointment.client.ProviderClient;
import com.medibook.appointment.client.ScheduleClient;
import com.medibook.appointment.dto.*;
import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.entity.AppointmentStatus;
import com.medibook.appointment.entity.ConsultationMode;
import com.medibook.appointment.exception.BusinessException;
import com.medibook.appointment.exception.ResourceNotFoundException;
import com.medibook.appointment.messaging.NotificationProducer;
import com.medibook.appointment.repository.AppointmentRepository;
import com.medibook.appointment.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    private static final String SERVICE_TYPE = "General Consultation";

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ScheduleClient scheduleClient;
    @Mock private NotificationProducer notificationProducer;
    @Mock private ProviderClient providerClient;

    @InjectMocks private AppointmentServiceImpl appointmentService;

    private SlotResponseDto futureSlot;
    private Appointment scheduledAppointment;

    @BeforeEach
    void setUp() {
        futureSlot = SlotResponseDto.builder()
                .slotId(10L).providerId(1L)
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30))
                .durationMinutes(30).isBooked(false).isBlocked(false)
                .build();

        scheduledAppointment = Appointment.builder()
                .appointmentId(1L).patientId(101L).providerId(1L).slotId(10L)
                .serviceType(SERVICE_TYPE)
                .appointmentDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30))
                .status(AppointmentStatus.SCHEDULED)
                .modeOfConsultation(ConsultationMode.IN_PERSON)
                .build();
    }

    // ── bookAppointment ────────────────────────────────────────────────────────

    @Test
    void bookAppointment_success() {
        BookAppointmentRequestDto req = BookAppointmentRequestDto.builder()
                .patientId(101L).providerId(1L).slotId(10L)
                .serviceType(SERVICE_TYPE).notes("Fever")
                .modeOfConsultation(ConsultationMode.IN_PERSON).build();

        when(scheduleClient.getSlotById(10L)).thenReturn(futureSlot);
        when(appointmentRepository.save(any())).thenReturn(scheduledAppointment);
        when(providerClient.getProviderById(1L)).thenReturn(
                ProviderResponseDto.builder().providerId(1L).userId(1L)
                        .fullName("Alice Smith").clinicName("City Clinic").clinicAddress("123 Main").build());
        doNothing().when(notificationProducer).publishNotification(any());

        AppointmentResponseDto result = appointmentService.bookAppointment(req);

        assertThat(result.getAppointmentId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        verify(scheduleClient).bookSlot(10L);
        verify(appointmentRepository).save(any());
        verify(notificationProducer, times(2)).publishNotification(any());
    }

    @Test
    void bookAppointment_slotNull_throws() {
        BookAppointmentRequestDto req = BookAppointmentRequestDto.builder()
                .patientId(101L).providerId(1L).slotId(99L)
                .serviceType(SERVICE_TYPE).modeOfConsultation(ConsultationMode.IN_PERSON).build();

        when(scheduleClient.getSlotById(99L)).thenReturn(null);

        assertThatThrownBy(() -> appointmentService.bookAppointment(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void bookAppointment_alreadyBooked_throws() {
        futureSlot.setIsBooked(true);
        BookAppointmentRequestDto req = BookAppointmentRequestDto.builder()
                .patientId(101L).providerId(1L).slotId(10L)
                .serviceType(SERVICE_TYPE).modeOfConsultation(ConsultationMode.IN_PERSON).build();

        when(scheduleClient.getSlotById(10L)).thenReturn(futureSlot);

        assertThatThrownBy(() -> appointmentService.bookAppointment(req))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already booked");
    }

    @Test
    void bookAppointment_blockedSlot_throws() {
        futureSlot.setIsBlocked(true);
        BookAppointmentRequestDto req = BookAppointmentRequestDto.builder()
                .patientId(101L).providerId(1L).slotId(10L)
                .serviceType(SERVICE_TYPE).modeOfConsultation(ConsultationMode.IN_PERSON).build();

        when(scheduleClient.getSlotById(10L)).thenReturn(futureSlot);

        assertThatThrownBy(() -> appointmentService.bookAppointment(req))
                .isInstanceOf(BusinessException.class).hasMessageContaining("blocked");
    }

    @Test
    void bookAppointment_pastDate_throws() {
        futureSlot.setDate(LocalDate.now().minusDays(1));
        BookAppointmentRequestDto req = BookAppointmentRequestDto.builder()
                .patientId(101L).providerId(1L).slotId(10L)
                .serviceType(SERVICE_TYPE).modeOfConsultation(ConsultationMode.IN_PERSON).build();

        when(scheduleClient.getSlotById(10L)).thenReturn(futureSlot);

        assertThatThrownBy(() -> appointmentService.bookAppointment(req))
                .isInstanceOf(BusinessException.class).hasMessageContaining("past");
    }

    @Test
    void bookAppointment_todaySlotElapsed_throws() {
        // Slot is today but start time is already in the past
        futureSlot.setDate(LocalDate.now());
        futureSlot.setStartTime(LocalTime.of(0, 1)); // 00:01 — definitely elapsed
        BookAppointmentRequestDto req = BookAppointmentRequestDto.builder()
                .patientId(101L).providerId(1L).slotId(10L)
                .serviceType(SERVICE_TYPE).modeOfConsultation(ConsultationMode.IN_PERSON).build();

        when(scheduleClient.getSlotById(10L)).thenReturn(futureSlot);

        assertThatThrownBy(() -> appointmentService.bookAppointment(req))
                .isInstanceOf(BusinessException.class).hasMessageContaining("passed");
    }

    @Test
    void bookAppointment_providerClientFailsGracefully() {
        BookAppointmentRequestDto req = BookAppointmentRequestDto.builder()
                .patientId(101L).providerId(1L).slotId(10L)
                .serviceType(SERVICE_TYPE).modeOfConsultation(ConsultationMode.IN_PERSON).build();

        when(scheduleClient.getSlotById(10L)).thenReturn(futureSlot);
        when(appointmentRepository.save(any())).thenReturn(scheduledAppointment);
        when(providerClient.getProviderById(1L)).thenThrow(new RuntimeException("Feign error"));
        doNothing().when(notificationProducer).publishNotification(any());

        // Should NOT throw — provider fetch failure is handled gracefully
        AppointmentResponseDto result = appointmentService.bookAppointment(req);
        assertThat(result).isNotNull();
    }

    // ── getById ────────────────────────────────────────────────────────────────

    @Test
    void getById_found() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(scheduledAppointment));
        assertThat(appointmentService.getById(1L).getAppointmentId()).isEqualTo(1L);
    }

    @Test
    void getById_notFound_throws() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> appointmentService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getByPatient / getByProvider ───────────────────────────────────────────

    @Test
    void getByPatient_returnsList() {
        when(appointmentRepository.findByPatientId(101L)).thenReturn(List.of(scheduledAppointment));
        assertThat(appointmentService.getByPatient(101L)).hasSize(1);
    }

    @Test
    void getByProvider_returnsList() {
        when(appointmentRepository.findByProviderId(1L)).thenReturn(List.of(scheduledAppointment));
        assertThat(appointmentService.getByProvider(1L)).hasSize(1);
    }

    @Test
    void getByProviderAndDate_returnsList() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(appointmentRepository.findByProviderIdAndAppointmentDate(1L, date))
                .thenReturn(List.of(scheduledAppointment));
        assertThat(appointmentService.getByProviderAndDate(1L, date)).hasSize(1);
    }

    // ── cancelAppointment ──────────────────────────────────────────────────────

    @Test
    void cancelAppointment_success() {
        Appointment cancelled = Appointment.builder()
                .appointmentId(1L).patientId(101L).providerId(1L).slotId(10L)
                .status(AppointmentStatus.CANCELLED).build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(scheduledAppointment));
        when(appointmentRepository.save(any())).thenReturn(cancelled);
        when(providerClient.getProviderById(1L)).thenReturn(
                ProviderResponseDto.builder().providerId(1L).userId(1L).fullName("Dr. Alice").build());
        doNothing().when(notificationProducer).publishNotification(any());

        AppointmentResponseDto result = appointmentService.cancelAppointment(1L);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(scheduleClient).unbookSlot(10L);
        verify(notificationProducer, times(2)).publishNotification(any());
    }

    @Test
    void cancelAppointment_alreadyCancelled_throws() {
        scheduledAppointment.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(scheduledAppointment));

        assertThatThrownBy(() -> appointmentService.cancelAppointment(1L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already cancelled");
    }

    @Test
    void cancelAppointment_completed_throws() {
        scheduledAppointment.setStatus(AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(scheduledAppointment));

        assertThatThrownBy(() -> appointmentService.cancelAppointment(1L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Completed");
    }

    @Test
    void cancelAppointment_notFound_throws() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> appointmentService.cancelAppointment(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── rescheduleAppointment ──────────────────────────────────────────────────

    @Test
    void rescheduleAppointment_success() {
        SlotResponseDto newSlot = SlotResponseDto.builder()
                .slotId(20L).providerId(1L)
                .date(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(11, 30))
                .isBooked(false).isBlocked(false).build();

        Appointment updated = Appointment.builder()
                .appointmentId(1L).patientId(101L).providerId(1L).slotId(20L)
                .appointmentDate(newSlot.getDate())
                .startTime(newSlot.getStartTime()).endTime(newSlot.getEndTime())
                .status(AppointmentStatus.SCHEDULED).build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(scheduledAppointment));
        when(scheduleClient.getSlotById(20L)).thenReturn(newSlot);
        when(appointmentRepository.save(any())).thenReturn(updated);
        when(providerClient.getProviderById(1L)).thenReturn(
                ProviderResponseDto.builder().providerId(1L).userId(1L).build());
        doNothing().when(notificationProducer).publishNotification(any());

        RescheduleAppointmentRequestDto req = RescheduleAppointmentRequestDto.builder()
                .newSlotId(20L).build();

        AppointmentResponseDto result = appointmentService.rescheduleAppointment(1L, req);

        assertThat(result.getSlotId()).isEqualTo(20L);
        verify(scheduleClient).bookSlot(20L);
        verify(scheduleClient).unbookSlot(10L);
        verify(notificationProducer, times(3)).publishNotification(any());
    }

    @Test
    void rescheduleAppointment_newSlotBooked_throws() {
        SlotResponseDto newSlot = SlotResponseDto.builder()
                .slotId(20L).isBooked(true).isBlocked(false).build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(scheduledAppointment));
        when(scheduleClient.getSlotById(20L)).thenReturn(newSlot);

        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(1L,
                RescheduleAppointmentRequestDto.builder().newSlotId(20L).build()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("not available");
    }

    @Test
    void rescheduleAppointment_cancelledAppointment_throws() {
        scheduledAppointment.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(scheduledAppointment));

        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(1L,
                RescheduleAppointmentRequestDto.builder().newSlotId(20L).build()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("scheduled");
    }

    @Test
    void rescheduleAppointment_newSlotNull_throws() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(scheduledAppointment));
        when(scheduleClient.getSlotById(20L)).thenReturn(null);

        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(1L,
                RescheduleAppointmentRequestDto.builder().newSlotId(20L).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── completeAppointment ────────────────────────────────────────────────────

    @Test
    void completeAppointment_success() {
        Appointment completed = Appointment.builder()
                .appointmentId(1L).status(AppointmentStatus.COMPLETED).build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(scheduledAppointment));
        when(appointmentRepository.save(any())).thenReturn(completed);

        AppointmentResponseDto result = appointmentService.completeAppointment(1L);
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void completeAppointment_notScheduled_throws() {
        scheduledAppointment.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(scheduledAppointment));

        assertThatThrownBy(() -> appointmentService.completeAppointment(1L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("scheduled");
    }

    @Test
    void completeAppointment_notFound_throws() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> appointmentService.completeAppointment(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateStatus ───────────────────────────────────────────────────────────

    @Test
    void updateStatus_success() {
        Appointment updated = Appointment.builder()
                .appointmentId(1L).status(AppointmentStatus.COMPLETED).build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(scheduledAppointment));
        when(appointmentRepository.save(any())).thenReturn(updated);

        AppointmentResponseDto result = appointmentService.updateStatus(1L, AppointmentStatus.COMPLETED);
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void updateStatus_notFound_throws() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> appointmentService.updateStatus(99L, AppointmentStatus.COMPLETED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getUpcomingByPatient ───────────────────────────────────────────────────

    @Test
    void getUpcomingByPatient_returnsList() {
        when(appointmentRepository.findByPatientIdAndStatus(101L, AppointmentStatus.SCHEDULED))
                .thenReturn(List.of(scheduledAppointment));
        assertThat(appointmentService.getUpcomingByPatient(101L)).hasSize(1);
    }

    // ── getAppointmentCount ────────────────────────────────────────────────────

    @Test
    void getAppointmentCount_returnsCount() {
        when(appointmentRepository.countByProviderId(1L)).thenReturn(5L);
        assertThat(appointmentService.getAppointmentCount(1L)).isEqualTo(5L);
    }

    // ── getAllAppointments ─────────────────────────────────────────────────────

    @Test
    void getAllAppointments_returnsList() {
        when(appointmentRepository.findAll()).thenReturn(List.of(scheduledAppointment, scheduledAppointment));
        assertThat(appointmentService.getAllAppointments()).hasSize(2);
    }
}
