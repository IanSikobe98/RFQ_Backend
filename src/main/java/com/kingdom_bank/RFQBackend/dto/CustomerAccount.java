package com.kingdom_bank.RFQBackend.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Getter
public class CustomerAccount {
    private String accountNumber;
    private String currency;
    private String accountName;
    private String balance;
//    private String schemeCode;
//    private String relationType;
    private String freezeCode;
    private String accountClosureFlag;
    private String phoneNumber;
    private String accountOpenDate;
    private String accountType;
    private String accountDescription;
    private String accountStatus;
    private String customerCif;
    private String accountCode;
    private String branchCode;
    private Boolean isOfficeAccount = false;
    private Boolean isStaffAccount = false;
    private String schemeCode;

    @Override
    public String toString() {
        return "CustomerAccount{" +
                "accountId='" + accountNumber + '\'' +
                ", currency='" + currency + '\'' +
                ", accountName='" + accountName + '\'' +
                ", balance='" + balance + '\'' +
                '}';
    }
}
