package com.kingdom_bank.RFQBackend.dto;

import com.kingdom_bank.RFQBackend.enums.AccountAction;
import lombok.Data;

@Data
public class CurrencyDirectionRequest {
    private String fromCurrency;
    private String toCurrency;
    private AccountAction action;
}
