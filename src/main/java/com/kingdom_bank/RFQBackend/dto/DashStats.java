package com.kingdom_bank.RFQBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashStats {
    private Double activeDeals = 0.0;
    private Double successRate = 0.0;
    private Double weekRate = 0.0;

}
