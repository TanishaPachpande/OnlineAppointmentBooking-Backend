package com.medibook.appointment.client;

import com.medibook.appointment.dto.SlotResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "SCHEDULE-SERVICE")
public interface ScheduleClient {

    @GetMapping("/slots/{slotId}")
    SlotResponseDto getSlotById(@PathVariable("slotId") Long slotId);

    @PutMapping("/slots/{slotId}/book")
    void bookSlot(@PathVariable("slotId") Long slotId);

    @PutMapping("/slots/{slotId}/unblock")
    void unblockSlot(@PathVariable("slotId") Long slotId);
}