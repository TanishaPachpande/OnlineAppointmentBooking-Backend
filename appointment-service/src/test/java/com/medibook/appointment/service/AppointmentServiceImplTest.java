package com.medibook.appointment.service;

import com.medibook.appointment.client.ScheduleClient;
import com.medibook.appointment.dto.BookAppointmentRequestDto;
import com.medibook.appointment.dto.AppointmentResponseDto;
import com.medibook.appointment.dto.RescheduleAppointmentRequestDto;
import com.medibook.appointment.dto.SlotResponseDto;
import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.entity.AppointmentStatus;
import com.medibook.appointment.entity.ConsultationMode;
import com.medibook.appointment.exception.BusinessException;
import com.medibook.appointment.messaging.NotificationProducer;
import com.medibook.appointment.repository.AppointmentRepository;
import com.medibook.appointment.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ScheduleClient scheduleClient;

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    @Test
    void bookAppointment_ShouldBookSuccessfully() {
        BookAppointmentRequestDto requestDto = BookAppointmentRequestDto.builder()
                .patientId(101L)
                .providerId(1L)
                .slotId(10L)
                .serviceType("General Consultation")
                .notes("Fever")
                .modeOfConsultation(ConsultationMode.IN_PERSON)
                .build();

        SlotResponseDto slot = SlotResponseDto.builder()
                .slotId(10L)
                .providerId(1L)
                .date(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .durationMinutes(30)
                .isBooked(false)
                .isBlocked(false)
                .build();

        Appointment savedAppointment = Appointment.builder()
                .appointmentId(1L)
                .patientId(101L)
                .providerId(1L)
                .slotId(10L)
                .serviceType("General Consultation")
                .appointmentDate(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(AppointmentStatus.SCHEDULED)
                .modeOfConsultation(ConsultationMode.IN_PERSON)
                .notes("Fever")
                .build();

        when(scheduleClient.getSlotById(10L)).thenReturn(slot);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

        AppointmentResponseDto response = appointmentService.bookAppointment(requestDto);

        assertNotNull(response);
        assertEquals(1L, response.getAppointmentId());
        assertEquals(AppointmentStatus.SCHEDULED, response.getStatus());

        verify(scheduleClient, times(1)).bookSlot(10L);
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
        verify(notificationProducer, times(1)).publishNotification(ArgumentMatchers.any());
    }

    @Test
    void bookAppointment_ShouldThrowException_WhenSlotAlreadyBooked() {
        BookAppointmentRequestDto requestDto = BookAppointmentRequestDto.builder()
                .patientId(101L)
                .providerId(1L)
                .slotId(10L)
                .serviceType("General Consultation")
                .modeOfConsultation(ConsultationMode.IN_PERSON)
                .build();

        SlotResponseDto slot = SlotResponseDto.builder()
                .slotId(10L)
                .isBooked(true)
                .isBlocked(false)
                .build();

        when(scheduleClient.getSlotById(10L)).thenReturn(slot);

        assertThrows(BusinessException.class, () -> appointmentService.bookAppointment(requestDto));

        verify(scheduleClient, never()).bookSlot(anyLong());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void cancelAppointment_ShouldCancelSuccessfully() {
        Appointment appointment = Appointment.builder()
                .appointmentId(1L)
                .patientId(101L)
                .slotId(10L)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment cancelledAppointment = Appointment.builder()
                .appointmentId(1L)
                .patientId(101L)
                .slotId(10L)
                .status(AppointmentStatus.CANCELLED)
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(cancelledAppointment);

        AppointmentResponseDto response = appointmentService.cancelAppointment(1L);

        assertEquals(AppointmentStatus.CANCELLED, response.getStatus());
        verify(scheduleClient, times(1)).unblockSlot(10L);
        verify(notificationProducer, times(1)).publishNotification(any());
    }

    @Test
    void rescheduleAppointment_ShouldRescheduleSuccessfully() {
        Appointment appointment = Appointment.builder()
                .appointmentId(1L)
                .patientId(101L)
                .slotId(10L)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        RescheduleAppointmentRequestDto requestDto = RescheduleAppointmentRequestDto.builder()
                .newSlotId(20L)
                .build();

        SlotResponseDto newSlot = SlotResponseDto.builder()
                .slotId(20L)
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(11, 30))
                .isBooked(false)
                .isBlocked(false)
                .build();

        Appointment updatedAppointment = Appointment.builder()
                .appointmentId(1L)
                .patientId(101L)
                .slotId(20L)
                .appointmentDate(newSlot.getDate())
                .startTime(newSlot.getStartTime())
                .endTime(newSlot.getEndTime())
                .status(AppointmentStatus.SCHEDULED)
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(scheduleClient.getSlotById(20L)).thenReturn(newSlot);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(updatedAppointment);

        AppointmentResponseDto response = appointmentService.rescheduleAppointment(1L, requestDto);

        assertEquals(20L, response.getSlotId());
        verify(scheduleClient).bookSlot(20L);
        verify(scheduleClient).unblockSlot(10L);
        verify(notificationProducer).publishNotification(any());
    }
}