package com.kingdom_bank.RFQBackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CreateRFQRequest {
    private String customerNo; //cif
    private String customerName;
    private String idNumber;
    private BigDecimal amount;
    private String fromCurrency;
    private String accountNumber;
    private String toCurrency;
    private String valueDate;
    private String negotiatedRate;
    private String tellerAccountName;

}
