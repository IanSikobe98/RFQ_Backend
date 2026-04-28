package com.kingdom_bank.RFQBackend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateAmountLimitConfigDTO {
    private BigDecimal amount;
}
