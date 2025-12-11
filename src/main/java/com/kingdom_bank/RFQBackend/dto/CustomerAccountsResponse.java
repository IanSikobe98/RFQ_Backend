package com.kingdom_bank.RFQBackend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerAccountsResponse {
    private String responseCode;
    private String responseMessage;
    private CustomerAccountSummary customerAccountSummary;
}
