package com.kingdom_bank.RFQBackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

@Data
@Valid
@Builder
public class AvailabilityItem {
    @NotEmpty(message = "Day is required")
    private String day;
    @NotEmpty(message = "Start Time is required")
    private String startTime;
    @NotEmpty(message = "End Time is required")
    private String endTime;
    @NotEmpty(message = "Status is required")
    private boolean enabled;
    private String key;
}
