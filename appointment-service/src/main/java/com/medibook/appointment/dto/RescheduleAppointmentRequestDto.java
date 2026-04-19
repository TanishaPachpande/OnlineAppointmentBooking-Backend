package com.medibook.appointment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleAppointmentRequestDto {

    @NotNull(message = "New slot ID is required")
    private Long newSlotId;
}