package com.kingdom_bank.RFQBackend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountDetailsDTO {
    private String customerCode;
    private String accountName;
    private String productId;
    private String currencyCode;
    private String balance;
    private String productName;
    private String productContextCode;
    private String branchCode;
    private String mobileNumber;
    private String accountOpenDate;
    private String accountStatus;

}
