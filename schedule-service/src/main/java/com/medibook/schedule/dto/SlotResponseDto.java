package com.medibook.schedule.dto;

import com.medibook.schedule.entity.RecurrenceType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotResponseDto {

    private Long slotId;
    private Long providerId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private Boolean isBooked;
    private Boolean isBlocked;
    private RecurrenceType recurrence;
}