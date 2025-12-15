package com.kingdom_bank.RFQBackend.dto;

import lombok.*;

@Getter @Setter @Data @AllArgsConstructor
@NoArgsConstructor
public class ExchangeRequest {
    private String fromCurrency;
    private String toCurrency;
    private String account;
    private String transactionAmount;
    private String channel;
}
