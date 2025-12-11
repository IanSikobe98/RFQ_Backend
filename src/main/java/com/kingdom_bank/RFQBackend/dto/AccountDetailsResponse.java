package com.kingdom_bank.RFQBackend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountDetailsResponse {
    private String responseCode;
    private String responseMessage;
    private AccountDetailsDTO accountDetails;

}