package com.medibook.schedule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medibook.schedule.dto.*;
import com.medibook.schedule.entity.RecurrenceType;
import com.medibook.schedule.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    @Mock private ScheduleService scheduleService;
    @InjectMocks private ScheduleController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private SlotResponseDto sampleSlot;
    private SlotRequestDto slotRequest;
    private final LocalDate FUTURE_DATE = LocalDate.now().plusDays(3);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleSlot = SlotResponseDto.builder()
                .slotId(1L).providerId(10L).date(FUTURE_DATE)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30))
                .durationMinutes(30).isBooked(false).isBlocked(false)
                .recurrence(RecurrenceType.NONE).build();

        slotRequest = SlotRequestDto.builder()
                .providerId(10L).date(FUTURE_DATE)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30))
                .durationMinutes(30).build();
    }

    @Test
    void addSlot_returns200() throws Exception {
        when(scheduleService.addSlot(any())).thenReturn(sampleSlot);

        mockMvc.perform(post("/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value(1L))
                .andExpect(jsonPath("$.isBooked").value(false));

        verify(scheduleService).addSlot(any());
    }

    @Test
    void addBulkSlots_returns200() throws Exception {
        BulkSlotRequestDto bulk = new BulkSlotRequestDto(List.of(slotRequest));
        when(scheduleService.addBulkSlots(any())).thenReturn(List.of(sampleSlot));

        mockMvc.perform(post("/slots/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulk)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void generateRecurringSlots_returns200() throws Exception {
        RecurringSlotRequestDto req = RecurringSlotRequestDto.builder()
                .providerId(10L).startDate(FUTURE_DATE).endDate(FUTURE_DATE.plusDays(1))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .durationMinutes(30).recurrenceType(RecurrenceType.DAILY).build();

        when(scheduleService.generateRecurringSlots(any())).thenReturn(List.of(sampleSlot, sampleSlot));

        mockMvc.perform(post("/slots/recurring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getSlotsByProvider_returns200() throws Exception {
        when(scheduleService.getSlotsByProvider(10L)).thenReturn(List.of(sampleSlot));

        mockMvc.perform(get("/slots/provider/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getSlotsByProviderAndDate_returns200() throws Exception {
        when(scheduleService.getSlotsByProviderAndDate(10L, FUTURE_DATE)).thenReturn(List.of(sampleSlot));

        mockMvc.perform(get("/slots/provider/10/date/" + FUTURE_DATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAvailableSlots_returns200() throws Exception {
        when(scheduleService.getAvailableSlots(10L, FUTURE_DATE)).thenReturn(List.of(sampleSlot));

        mockMvc.perform(get("/slots/available/10/" + FUTURE_DATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getSlotById_returns200() throws Exception {
        when(scheduleService.getSlotById(1L)).thenReturn(sampleSlot);

        mockMvc.perform(get("/slots/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value(10L));
    }

    @Test
    void bookSlot_returns200() throws Exception {
        when(scheduleService.bookSlot(1L)).thenReturn(sampleSlot);

        mockMvc.perform(put("/slots/1/book"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value(1L));
    }

    @Test
    void blockSlot_returns200() throws Exception {
        when(scheduleService.blockSlot(1L)).thenReturn(sampleSlot);

        mockMvc.perform(put("/slots/1/block"))
                .andExpect(status().isOk());
    }

    @Test
    void unblockSlot_returns200() throws Exception {
        when(scheduleService.unblockSlot(1L)).thenReturn(sampleSlot);

        mockMvc.perform(put("/slots/1/unblock"))
                .andExpect(status().isOk());
    }

    @Test
    void unbookSlot_returns200() throws Exception {
        when(scheduleService.unbookSlot(1L)).thenReturn(sampleSlot);

        mockMvc.perform(put("/slots/1/unbook"))
                .andExpect(status().isOk());
    }

    @Test
    void updateSlot_returns200() throws Exception {
        when(scheduleService.updateSlot(eq(1L), any())).thenReturn(sampleSlot);

        mockMvc.perform(put("/slots/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value(1L));
    }

    @Test
    void deleteSlot_returns200() throws Exception {
        when(scheduleService.deleteSlot(1L)).thenReturn("Slot deleted successfully");

        mockMvc.perform(delete("/slots/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Slot deleted successfully"));
    }

    @Test
    void getAvailableSlots_emptyList_returns200() throws Exception {
        when(scheduleService.getAvailableSlots(10L, FUTURE_DATE)).thenReturn(List.of());

        mockMvc.perform(get("/slots/available/10/" + FUTURE_DATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
