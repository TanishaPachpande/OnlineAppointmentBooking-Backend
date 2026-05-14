package com.medibook.schedule.controller;

import com.medibook.schedule.dto.*;
import com.medibook.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/slots")
@Tag(name = "Schedule Controller", description = "APIs for slot management")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ResponseEntity<SlotResponseDto> addSlot(@Valid @RequestBody SlotRequestDto requestDto) {
        log.info("API CALL: Add Slot for providerId={}", requestDto.getProviderId());
        return ResponseEntity.ok(scheduleService.addSlot(requestDto));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<SlotResponseDto>> addBulkSlots(@Valid @RequestBody BulkSlotRequestDto requestDto) {
        log.info("API CALL: Add Bulk Slots");
        return ResponseEntity.ok(scheduleService.addBulkSlots(requestDto));
    }

    @PostMapping("/recurring")
    public ResponseEntity<List<SlotResponseDto>> generateRecurringSlots(
            @Valid @RequestBody RecurringSlotRequestDto requestDto) {
        log.info("API CALL: Generate Recurring Slots for providerId={}", requestDto.getProviderId());
        return ResponseEntity.ok(scheduleService.generateRecurringSlots(requestDto));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<SlotResponseDto>> getSlotsByProvider(@PathVariable Long providerId) {
        return ResponseEntity.ok(scheduleService.getSlotsByProvider(providerId));
    }

    @GetMapping("/provider/{providerId}/date/{date}")
    public ResponseEntity<List<SlotResponseDto>> getSlotsByProviderAndDate(@PathVariable Long providerId,
                                                                           @PathVariable LocalDate date) {
        return ResponseEntity.ok(scheduleService.getSlotsByProviderAndDate(providerId, date));
    }

    @GetMapping("/available/{providerId}/{date}")
    public ResponseEntity<List<SlotResponseDto>> getAvailableSlots(@PathVariable Long providerId,
                                                                   @PathVariable LocalDate date) {
        return ResponseEntity.ok(scheduleService.getAvailableSlots(providerId, date));
    }

    @GetMapping("/{slotId}")
    public ResponseEntity<SlotResponseDto> getSlotById(@PathVariable Long slotId) {
        return ResponseEntity.ok(scheduleService.getSlotById(slotId));
    }

    @PutMapping("/{slotId}/book")
    public ResponseEntity<SlotResponseDto> bookSlot(@PathVariable Long slotId) {
        return ResponseEntity.ok(scheduleService.bookSlot(slotId));
    }

    @PutMapping("/{slotId}/block")
    public ResponseEntity<SlotResponseDto> blockSlot(@PathVariable Long slotId) {
        return ResponseEntity.ok(scheduleService.blockSlot(slotId));
    }

    @PutMapping("/{slotId}/unblock")
    public ResponseEntity<SlotResponseDto> unblockSlot(@PathVariable Long slotId) {
        return ResponseEntity.ok(scheduleService.unblockSlot(slotId));
    }

    @PutMapping("/{slotId}/unbook")
    public ResponseEntity<SlotResponseDto> unbookSlot(@PathVariable Long slotId) {
        return ResponseEntity.ok(scheduleService.unbookSlot(slotId));
    }

    @PutMapping("/{slotId}")
    public ResponseEntity<SlotResponseDto> updateSlot(@PathVariable Long slotId,
                                                      @Valid @RequestBody SlotRequestDto requestDto) {
        return ResponseEntity.ok(scheduleService.updateSlot(slotId, requestDto));
    }

    @DeleteMapping("/{slotId}")
    public ResponseEntity<ApiResponseDto> deleteSlot(@PathVariable Long slotId) {
        return ResponseEntity.ok(
                ApiResponseDto.builder()
                        .message(scheduleService.deleteSlot(slotId))
                        .build()
        );
    }
}