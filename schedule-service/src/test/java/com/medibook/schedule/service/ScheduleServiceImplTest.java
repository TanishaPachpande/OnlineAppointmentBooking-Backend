package com.medibook.schedule.service;

import com.medibook.schedule.dto.BulkSlotRequestDto;
import com.medibook.schedule.dto.RecurringSlotRequestDto;
import com.medibook.schedule.dto.SlotRequestDto;
import com.medibook.schedule.dto.SlotResponseDto;
import com.medibook.schedule.entity.AvailabilitySlot;
import com.medibook.schedule.entity.RecurrenceType;
import com.medibook.schedule.exception.InvalidSlotException;
import com.medibook.schedule.exception.ResourceNotFoundException;
import com.medibook.schedule.repository.SlotRepository;
import com.medibook.schedule.service.impl.ScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock private SlotRepository slotRepository;
    @InjectMocks private ScheduleServiceImpl scheduleService;

    private AvailabilitySlot freeSlot;
    private SlotRequestDto slotRequest;
    private final LocalDate FUTURE_DATE = LocalDate.now().plusDays(3);

    @BeforeEach
    void setUp() {
        freeSlot = AvailabilitySlot.builder()
                .slotId(1L).providerId(10L).date(FUTURE_DATE)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30))
                .durationMinutes(30).isBooked(false).isBlocked(false)
                .recurrence(RecurrenceType.NONE).createdAt(LocalDateTime.now()).build();

        slotRequest = SlotRequestDto.builder()
                .providerId(10L).date(FUTURE_DATE)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30))
                .durationMinutes(30).build();
    }

    // ── addSlot ────────────────────────────────────────────────────────────────

    @Test
    void addSlot_success() {
        when(slotRepository.existsByProviderIdAndDateAndStartTime(10L, FUTURE_DATE, LocalTime.of(9, 0))).thenReturn(false);
        when(slotRepository.save(any())).thenReturn(freeSlot);

        SlotResponseDto result = scheduleService.addSlot(slotRequest);

        assertThat(result.getSlotId()).isEqualTo(1L);
        assertThat(result.getIsBooked()).isFalse();
        verify(slotRepository).save(any());
    }

    @Test
    void addSlot_pastDate_throws() {
        slotRequest.setDate(LocalDate.now().minusDays(1));
        assertThatThrownBy(() -> scheduleService.addSlot(slotRequest))
                .isInstanceOf(InvalidSlotException.class).hasMessageContaining("past");
    }

    @Test
    void addSlot_endBeforeStart_throws() {
        slotRequest.setEndTime(LocalTime.of(8, 0));
        assertThatThrownBy(() -> scheduleService.addSlot(slotRequest))
                .isInstanceOf(InvalidSlotException.class).hasMessageContaining("End time");
    }

    @Test
    void addSlot_duplicate_throws() {
        when(slotRepository.existsByProviderIdAndDateAndStartTime(10L, FUTURE_DATE, LocalTime.of(9, 0))).thenReturn(true);
        assertThatThrownBy(() -> scheduleService.addSlot(slotRequest))
                .isInstanceOf(InvalidSlotException.class).hasMessageContaining("already exists");
    }

    // ── addBulkSlots ───────────────────────────────────────────────────────────

    @Test
    void addBulkSlots_createsMultipleSlots() {
        SlotRequestDto req2 = SlotRequestDto.builder()
                .providerId(10L).date(FUTURE_DATE)
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30))
                .durationMinutes(30).build();

        BulkSlotRequestDto bulk = new BulkSlotRequestDto(List.of(slotRequest, req2));

        when(slotRepository.existsByProviderIdAndDateAndStartTime(any(), any(), any())).thenReturn(false);
        when(slotRepository.save(any())).thenReturn(freeSlot);

        List<SlotResponseDto> result = scheduleService.addBulkSlots(bulk);
        assertThat(result).hasSize(2);
    }

    // ── getSlotById ────────────────────────────────────────────────────────────

    @Test
    void getSlotById_found() {
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlot));
        assertThat(scheduleService.getSlotById(1L).getSlotId()).isEqualTo(1L);
    }

    @Test
    void getSlotById_notFound_throws() {
        when(slotRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> scheduleService.getSlotById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── bookSlot ───────────────────────────────────────────────────────────────

    @Test
    void bookSlot_success() {
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlot));
        when(slotRepository.save(any())).thenReturn(freeSlot);

        SlotResponseDto result = scheduleService.bookSlot(1L);
        assertThat(result).isNotNull();
        verify(slotRepository).save(any());
    }

    @Test
    void bookSlot_alreadyBooked_throws() {
        freeSlot.setIsBooked(true);
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlot));

        assertThatThrownBy(() -> scheduleService.bookSlot(1L))
                .isInstanceOf(InvalidSlotException.class).hasMessageContaining("not available");
    }

    @Test
    void bookSlot_blocked_throws() {
        freeSlot.setIsBlocked(true);
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlot));

        assertThatThrownBy(() -> scheduleService.bookSlot(1L))
                .isInstanceOf(InvalidSlotException.class);
    }

    @Test
    void bookSlot_pastDate_throws() {
        freeSlot.setDate(LocalDate.now().minusDays(1));
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlot));

        assertThatThrownBy(() -> scheduleService.bookSlot(1L))
                .isInstanceOf(InvalidSlotException.class).hasMessageContaining("past");
    }

    @Test
    void bookSlot_todayElapsed_throws() {
        freeSlot.setDate(LocalDate.now());
        freeSlot.setStartTime(LocalTime.of(0, 1)); // definitely elapsed
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlot));

        assertThatThrownBy(() -> scheduleService.bookSlot(1L))
                .isInstanceOf(InvalidSlotException.class).hasMessageContaining("passed");
    }

    @Test
    void bookSlot_notFound_throws() {
        when(slotRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> scheduleService.bookSlot(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── unbookSlot / unblockSlot / blockSlot ──────────────────────────────────

    @Test
    void unbookSlot_success() {
        freeSlot.setIsBooked(true);
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlot));
        when(slotRepository.save(any())).thenReturn(freeSlot);

        SlotResponseDto result = scheduleService.unbookSlot(1L);
        assertThat(result).isNotNull();
        verify(slotRepository).save(argThat(s -> !s.getIsBooked()));
    }

    @Test
    void unbookSlot_notFound_throws() {
        when(slotRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> scheduleService.unbookSlot(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void blockSlot_success() {
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlot));
        when(slotRepository.save(any())).thenReturn(freeSlot);

        SlotResponseDto result = scheduleService.blockSlot(1L);
        assertThat(result).isNotNull();
        verify(slotRepository).save(argThat(AvailabilitySlot::getIsBlocked));
    }

    @Test
    void blockSlot_alreadyBooked_throws() {
        freeSlot.setIsBooked(true);
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlot));

        assertThatThrownBy(() -> scheduleService.blockSlot(1L))
                .isInstanceOf(InvalidSlotException.class).hasMessageContaining("already booked");
    }

    @Test
    void blockSlot_notFound_throws() {
        when(slotRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> scheduleService.blockSlot(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unblockSlot_success() {
        freeSlot.setIsBlocked(true);
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlot));
        when(slotRepository.save(any())).thenReturn(freeSlot);

        SlotResponseDto result = scheduleService.unblockSlot(1L);
        assertThat(result).isNotNull();
        verify(slotRepository).save(argThat(s -> !s.getIsBlocked() && !s.getIsBooked()));
    }

    @Test
    void unblockSlot_notFound_throws() {
        when(slotRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> scheduleService.unblockSlot(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateSlot ─────────────────────────────────────────────────────────────

    @Test
    void updateSlot_success() {
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlot));
        when(slotRepository.save(any())).thenReturn(freeSlot);

        SlotResponseDto result = scheduleService.updateSlot(1L, slotRequest);
        assertThat(result).isNotNull();
    }

    @Test
    void updateSlot_bookedSlot_throws() {
        freeSlot.setIsBooked(true);
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlot));

        assertThatThrownBy(() -> scheduleService.updateSlot(1L, slotRequest))
                .isInstanceOf(InvalidSlotException.class).hasMessageContaining("Booked");
    }

    @Test
    void updateSlot_notFound_throws() {
        when(slotRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> scheduleService.updateSlot(99L, slotRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteSlot ─────────────────────────────────────────────────────────────

    @Test
    void deleteSlot_success() {
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlot));
        doNothing().when(slotRepository).delete(freeSlot);

        assertThat(scheduleService.deleteSlot(1L)).contains("deleted");
        verify(slotRepository).delete(freeSlot);
    }

    @Test
    void deleteSlot_bookedSlot_throws() {
        freeSlot.setIsBooked(true);
        when(slotRepository.findById(1L)).thenReturn(Optional.of(freeSlot));

        assertThatThrownBy(() -> scheduleService.deleteSlot(1L))
                .isInstanceOf(InvalidSlotException.class).hasMessageContaining("Cannot delete");
    }

    @Test
    void deleteSlot_notFound_throws() {
        when(slotRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> scheduleService.deleteSlot(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── query methods ──────────────────────────────────────────────────────────

    @Test
    void getSlotsByProvider_returnsList() {
        when(slotRepository.findByProviderId(10L)).thenReturn(List.of(freeSlot));
        assertThat(scheduleService.getSlotsByProvider(10L)).hasSize(1);
    }

    @Test
    void getSlotsByProviderAndDate_returnsList() {
        when(slotRepository.findByProviderIdAndDate(10L, FUTURE_DATE)).thenReturn(List.of(freeSlot));
        assertThat(scheduleService.getSlotsByProviderAndDate(10L, FUTURE_DATE)).hasSize(1);
    }

    @Test
    void getAvailableSlots_futureDate_returnsAll() {
        when(slotRepository.findByProviderIdAndDateAndIsBookedFalseAndIsBlockedFalse(10L, FUTURE_DATE))
                .thenReturn(List.of(freeSlot));
        assertThat(scheduleService.getAvailableSlots(10L, FUTURE_DATE)).hasSize(1);
    }

    @Test
    void getAvailableSlots_todayElapsedSlot_filtered() {
        // A slot for today with a startTime that has already passed (00:01)
        AvailabilitySlot elapsedSlot = AvailabilitySlot.builder()
                .slotId(2L).providerId(10L).date(LocalDate.now())
                .startTime(LocalTime.of(0, 1)).endTime(LocalTime.of(0, 30))
                .isBooked(false).isBlocked(false).build();

        when(slotRepository.findByProviderIdAndDateAndIsBookedFalseAndIsBlockedFalse(10L, LocalDate.now()))
                .thenReturn(List.of(elapsedSlot));

        // Should return empty because the slot's startTime is already in the past
        assertThat(scheduleService.getAvailableSlots(10L, LocalDate.now())).isEmpty();
    }

    // ── expirePastSlots (scheduler) ────────────────────────────────────────────

    @Test
    void expirePastSlots_blocksOldSlotsAndTodayElapsed() {
        AvailabilitySlot todayElapsed = AvailabilitySlot.builder()
                .slotId(3L).providerId(10L).date(LocalDate.now())
                .startTime(LocalTime.of(0, 1)).isBooked(false).isBlocked(false).build();

        when(slotRepository.blockAllExpiredUnbookedSlots(any())).thenReturn(2);
        when(slotRepository.findTodayExpiredUnbookedSlots(any(), any()))
                .thenReturn(List.of(todayElapsed));
        when(slotRepository.saveAll(any())).thenReturn(List.of(todayElapsed));

        scheduleService.expirePastSlots();

        verify(slotRepository).blockAllExpiredUnbookedSlots(any());
        verify(slotRepository).findTodayExpiredUnbookedSlots(any(), any());
        verify(slotRepository).saveAll(any());
    }

    @Test
    void expirePastSlots_noExpiredSlots_noSaveAll() {
        when(slotRepository.blockAllExpiredUnbookedSlots(any())).thenReturn(0);
        when(slotRepository.findTodayExpiredUnbookedSlots(any(), any())).thenReturn(List.of());

        scheduleService.expirePastSlots();

        verify(slotRepository, never()).saveAll(any());
    }

    // ── generateRecurringSlots ─────────────────────────────────────────────────

    @Test
    void generateRecurringSlots_endBeforeStart_throws() {
        RecurringSlotRequestDto req = RecurringSlotRequestDto.builder()
                .providerId(10L).startDate(FUTURE_DATE).endDate(FUTURE_DATE.minusDays(1))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(11, 0))
                .durationMinutes(30).recurrenceType(RecurrenceType.DAILY).build();

        assertThatThrownBy(() -> scheduleService.generateRecurringSlots(req))
                .isInstanceOf(InvalidSlotException.class).hasMessageContaining("End date");
    }

    @Test
    void generateRecurringSlots_daily_createsSlots() {
        RecurringSlotRequestDto req = RecurringSlotRequestDto.builder()
                .providerId(10L).startDate(FUTURE_DATE).endDate(FUTURE_DATE.plusDays(1))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .durationMinutes(30).recurrenceType(RecurrenceType.DAILY).build();

        when(slotRepository.existsByProviderIdAndDateAndStartTime(any(), any(), any())).thenReturn(false);
        when(slotRepository.save(any())).thenReturn(freeSlot);

        List<SlotResponseDto> results = scheduleService.generateRecurringSlots(req);
        assertThat(results).isNotEmpty();
    }

    @Test
    void generateRecurringSlots_weekly_createsSlots() {
        RecurringSlotRequestDto req = RecurringSlotRequestDto.builder()
                .providerId(10L).startDate(FUTURE_DATE).endDate(FUTURE_DATE.plusWeeks(1))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .durationMinutes(30).recurrenceType(RecurrenceType.WEEKLY).build();

        when(slotRepository.existsByProviderIdAndDateAndStartTime(any(), any(), any())).thenReturn(false);
        when(slotRepository.save(any())).thenReturn(freeSlot);

        List<SlotResponseDto> results = scheduleService.generateRecurringSlots(req);
        assertThat(results).isNotEmpty();
    }

    @Test
    void generateRecurringSlots_skipsExistingSlots() {
        RecurringSlotRequestDto req = RecurringSlotRequestDto.builder()
                .providerId(10L).startDate(FUTURE_DATE).endDate(FUTURE_DATE)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .durationMinutes(30).recurrenceType(RecurrenceType.DAILY).build();

        // All slots already exist
        when(slotRepository.existsByProviderIdAndDateAndStartTime(any(), any(), any())).thenReturn(true);

        List<SlotResponseDto> results = scheduleService.generateRecurringSlots(req);
        assertThat(results).isEmpty();
        verify(slotRepository, never()).save(any());
    }
}
