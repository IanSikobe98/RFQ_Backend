package com.kingdom_bank.RFQBackend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerAccountSummary {
    private String phoneNumber;
    private String fullName;
    private String joiningYear;
    private String customerCif;
    private String idNumber;
    private List<CustomerAccount> accounts;

    public CustomerAccountSummary(String phoneNumber, String fullName, String joiningYear, String customerCif, List<CustomerAccount> accounts) {
        this.phoneNumber = phoneNumber;
        this.fullName = fullName;
        this.joiningYear = joiningYear;
        this.customerCif = customerCif;
        this.accounts = accounts;
    }


}
