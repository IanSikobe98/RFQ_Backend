package com.kingdom_bank.RFQBackend.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
@Valid
public class UpdateScheduleDTO {
    List<AvailabilityItem> items;

}
