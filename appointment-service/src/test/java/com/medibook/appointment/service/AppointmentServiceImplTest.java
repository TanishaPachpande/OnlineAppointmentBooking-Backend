package com.medibook.appointment.service;

import com.medibook.appointment.client.ScheduleClient;
import com.medibook.appointment.dto.*;
import com.medibook.appointment.entity.*;
import com.medibook.appointment.exception.BusinessException;
import com.medibook.appointment.repository.AppointmentRepository;
import com.medibook.appointment.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
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
                .slotId(10L)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment cancelledAppointment = Appointment.builder()
                .appointmentId(1L)
                .slotId(10L)
                .status(AppointmentStatus.CANCELLED)
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(cancelledAppointment);

        AppointmentResponseDto response = appointmentService.cancelAppointment(1L);

        assertEquals(AppointmentStatus.CANCELLED, response.getStatus());
        verify(scheduleClient, times(1)).unblockSlot(10L);
    }
}