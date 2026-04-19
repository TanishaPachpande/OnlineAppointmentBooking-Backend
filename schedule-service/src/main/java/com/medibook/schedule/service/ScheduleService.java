package com.medibook.schedule.service;

import com.medibook.schedule.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {

    SlotResponseDto addSlot(SlotRequestDto requestDto);

    List<SlotResponseDto> addBulkSlots(BulkSlotRequestDto requestDto);

    List<SlotResponseDto> generateRecurringSlots(RecurringSlotRequestDto requestDto);

    List<SlotResponseDto> getSlotsByProvider(Long providerId);

    List<SlotResponseDto> getSlotsByProviderAndDate(Long providerId, LocalDate date);

    List<SlotResponseDto> getAvailableSlots(Long providerId, LocalDate date);

    SlotResponseDto getSlotById(Long slotId);

    SlotResponseDto bookSlot(Long slotId);

    SlotResponseDto unblockSlot(Long slotId);

    SlotResponseDto blockSlot(Long slotId);

    SlotResponseDto updateSlot(Long slotId, SlotRequestDto requestDto);

    String deleteSlot(Long slotId);
}