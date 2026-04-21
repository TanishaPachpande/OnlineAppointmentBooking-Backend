package com.medibook.schedule.service;

import com.medibook.schedule.dto.SlotRequestDto;
import com.medibook.schedule.dto.SlotResponseDto;
import com.medibook.schedule.entity.AvailabilitySlot;
import com.medibook.schedule.exception.InvalidSlotException;
import com.medibook.schedule.repository.SlotRepository;
import com.medibook.schedule.service.impl.ScheduleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    private SlotRepository scheduleSlotRepository;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    @Test
    void addSlot_ShouldCreateSuccessfully() {
        SlotRequestDto requestDto = SlotRequestDto.builder()
                .providerId(1L)
                .date(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .durationMinutes(30)
                .build();

        AvailabilitySlot savedSlot = AvailabilitySlot.builder()
                .slotId(1L)
                .providerId(1L)
                .date(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .durationMinutes(30)
                .isBooked(false)
                .isBlocked(false)
                .build();

        when(scheduleSlotRepository.save(any(AvailabilitySlot.class))).thenReturn(savedSlot);

        SlotResponseDto response = scheduleService.addSlot(requestDto);

        assertNotNull(response);
        assertEquals(1L, response.getSlotId());
        assertFalse(response.getIsBooked());
        assertFalse(response.getIsBlocked());
    }

    @Test
    void blockSlot_ShouldBlockSuccessfully() {
        AvailabilitySlot slot = AvailabilitySlot.builder()
                .slotId(1L)
                .isBooked(false)
                .isBlocked(false)
                .build();

        AvailabilitySlot updatedSlot = AvailabilitySlot.builder()
                .slotId(1L)
                .isBooked(false)
                .isBlocked(true)
                .build();

        when(scheduleSlotRepository.findById(1L)).thenReturn(Optional.of(slot));
        when(scheduleSlotRepository.save(any(AvailabilitySlot.class))).thenReturn(updatedSlot);

        SlotResponseDto response = scheduleService.blockSlot(1L);

        assertNotNull(response);
        assertTrue(response.getIsBlocked());
    }

    @Test
    void blockSlot_ShouldThrowException_WhenAlreadyBooked() {
        AvailabilitySlot slot = AvailabilitySlot.builder()
                .slotId(1L)
                .isBooked(true)
                .isBlocked(false)
                .build();

        when(scheduleSlotRepository.findById(1L)).thenReturn(Optional.of(slot));

        assertThrows(InvalidSlotException.class, () -> scheduleService.blockSlot(1L));
    }

    @Test
    void getAvailableSlots_ShouldReturnOnlyAvailableSlots() {
        AvailabilitySlot slot1 = AvailabilitySlot.builder()
                .slotId(1L)
                .providerId(1L)
                .date(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .durationMinutes(30)
                .isBooked(false)
                .isBlocked(false)
                .build();

        when(scheduleSlotRepository.findByProviderIdAndDateAndIsBookedFalseAndIsBlockedFalse(1L, LocalDate.now()))
                .thenReturn(List.of(slot1));

        List<SlotResponseDto> responses = scheduleService.getAvailableSlots(1L, LocalDate.now());

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getSlotId());
    }
}