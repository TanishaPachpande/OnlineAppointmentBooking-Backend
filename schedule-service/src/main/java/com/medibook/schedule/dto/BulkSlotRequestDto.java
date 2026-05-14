package com.medibook.schedule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkSlotRequestDto {

    @Valid
    @NotEmpty(message = "Slot list cannot be empty")
    private List<SlotRequestDto> slots;
}