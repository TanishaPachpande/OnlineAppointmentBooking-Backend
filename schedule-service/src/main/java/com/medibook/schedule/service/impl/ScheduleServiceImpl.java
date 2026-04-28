package com.medibook.schedule.service.impl;

import com.medibook.schedule.dto.*;
import com.medibook.schedule.entity.AvailabilitySlot;
import com.medibook.schedule.entity.RecurrenceType;
import com.medibook.schedule.exception.InvalidSlotException;
import com.medibook.schedule.exception.ResourceNotFoundException;
import com.medibook.schedule.repository.SlotRepository;
import com.medibook.schedule.service.ScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {

    private final SlotRepository slotRepository;

    public ScheduleServiceImpl(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @Override
    public SlotResponseDto addSlot(SlotRequestDto requestDto) {
        validateSlotTime(requestDto);

        if (requestDto.getDate().isBefore(LocalDate.now())) {
            throw new InvalidSlotException("Cannot create slots for past dates.");
        }

        // Check for duplicates before saving
        if (slotRepository.existsByProviderIdAndDateAndStartTime(
                requestDto.getProviderId(), requestDto.getDate(), requestDto.getStartTime())) {
            throw new InvalidSlotException("A slot already exists for this time and date.");
        }

        log.info("Adding slot for providerId={} on date={}", requestDto.getProviderId(), requestDto.getDate());

        AvailabilitySlot slot = AvailabilitySlot.builder()
                .providerId(requestDto.getProviderId())
                .date(requestDto.getDate())
                .startTime(requestDto.getStartTime())
                .endTime(requestDto.getEndTime())
                .durationMinutes(requestDto.getDurationMinutes())
                .recurrence(RecurrenceType.NONE)
                .isBooked(false)
                .isBlocked(false)
                .build();

        return mapToResponse(slotRepository.save(slot));
    }

    @Override
    public List<SlotResponseDto> addBulkSlots(BulkSlotRequestDto requestDto) {
        List<SlotResponseDto> responseList = new ArrayList<>();
        for (SlotRequestDto slotRequestDto : requestDto.getSlots()) {
            responseList.add(addSlot(slotRequestDto));
        }
        return responseList;
    }

    @Override
    public List<SlotResponseDto> generateRecurringSlots(RecurringSlotRequestDto requestDto) {
        if (requestDto.getEndDate().isBefore(requestDto.getStartDate())) {
            throw new InvalidSlotException("End date cannot be before start date");
        }

        List<SlotResponseDto> responseList = new ArrayList<>();
        LocalDate currentDate = requestDto.getStartDate();

        while (!currentDate.isAfter(requestDto.getEndDate())) {
            LocalTime slotStartTime = requestDto.getStartTime();

            while (slotStartTime.plusMinutes(requestDto.getDurationMinutes()).isBefore(requestDto.getEndTime())
                    || slotStartTime.plusMinutes(requestDto.getDurationMinutes()).equals(requestDto.getEndTime())) {

                LocalTime slotEndTime = slotStartTime.plusMinutes(requestDto.getDurationMinutes());

                // Skip generation if slot already exists to prevent partial failure
                if (!slotRepository.existsByProviderIdAndDateAndStartTime(requestDto.getProviderId(), currentDate, slotStartTime)) {
                    AvailabilitySlot slot = AvailabilitySlot.builder()
                            .providerId(requestDto.getProviderId())
                            .date(currentDate)
                            .startTime(slotStartTime)
                            .endTime(slotEndTime)
                            .durationMinutes(requestDto.getDurationMinutes())
                            .recurrence(requestDto.getRecurrenceType())
                            .isBooked(false)
                            .isBlocked(false)
                            .build();

                    responseList.add(mapToResponse(slotRepository.save(slot)));
                }

                slotStartTime = slotEndTime;
            }

            if (requestDto.getRecurrenceType() == RecurrenceType.DAILY) {
                currentDate = currentDate.plusDays(1);
            } else if (requestDto.getRecurrenceType() == RecurrenceType.WEEKLY) {
                currentDate = currentDate.plusWeeks(1);
            } else {
                break;
            }
        }
        return responseList;
    }

    @Override
    public List<SlotResponseDto> getSlotsByProvider(Long providerId) {
        return slotRepository.findByProviderId(providerId).stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<SlotResponseDto> getSlotsByProviderAndDate(Long providerId, LocalDate date) {
        return slotRepository.findByProviderIdAndDate(providerId, date).stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<SlotResponseDto> getAvailableSlots(Long providerId, LocalDate date) {
        return slotRepository.findByProviderIdAndDateAndIsBookedFalseAndIsBlockedFalse(providerId, date)
                .stream().map(this::mapToResponse).toList();
    }

    @Override
    public SlotResponseDto getSlotById(Long slotId) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with id: " + slotId));
        return mapToResponse(slot);
    }

    @Override
    public SlotResponseDto bookSlot(Long slotId) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));

        if (Boolean.TRUE.equals(slot.getIsBlocked()) || Boolean.TRUE.equals(slot.getIsBooked())) {
            throw new InvalidSlotException("Slot is not available for booking");
        }

        slot.setIsBooked(true);
        return mapToResponse(slotRepository.save(slot));
    }

    @Override
    public SlotResponseDto unblockSlot(Long slotId) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        slot.setIsBlocked(false);
        slot.setIsBooked(false);
        return mapToResponse(slotRepository.save(slot));
    }

    @Override
    public SlotResponseDto blockSlot(Long slotId) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));

        if (Boolean.TRUE.equals(slot.getIsBooked())) {
            throw new InvalidSlotException("Cannot block a slot that is already booked");
        }

        slot.setIsBlocked(true);
        return mapToResponse(slotRepository.save(slot));
    }

    @Override
    public SlotResponseDto updateSlot(Long slotId, SlotRequestDto requestDto) {
        validateSlotTime(requestDto);
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));

        if (Boolean.TRUE.equals(slot.getIsBooked())) {
            throw new InvalidSlotException("Booked slots cannot be modified");
        }

        slot.setDate(requestDto.getDate());
        slot.setStartTime(requestDto.getStartTime());
        slot.setEndTime(requestDto.getEndTime());
        slot.setDurationMinutes(requestDto.getDurationMinutes());

        return mapToResponse(slotRepository.save(slot));
    }

    @Override
    public String deleteSlot(Long slotId) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));

        if (Boolean.TRUE.equals(slot.getIsBooked())) {
            throw new InvalidSlotException("Cannot delete a booked slot. Please cancel the appointment first.");
        }

        slotRepository.delete(slot);
        return "Slot deleted successfully";
    }

    private void validateSlotTime(SlotRequestDto requestDto) {
        if (!requestDto.getEndTime().isAfter(requestDto.getStartTime())) {
            throw new InvalidSlotException("End time must be after start time");
        }
    }

    private SlotResponseDto mapToResponse(AvailabilitySlot slot) {
        return SlotResponseDto.builder()
                .slotId(slot.getSlotId())
                .providerId(slot.getProviderId())
                .date(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .durationMinutes(slot.getDurationMinutes())
                .isBooked(slot.getIsBooked())
                .isBlocked(slot.getIsBlocked())
                .recurrence(slot.getRecurrence())
                .build();
    }
}