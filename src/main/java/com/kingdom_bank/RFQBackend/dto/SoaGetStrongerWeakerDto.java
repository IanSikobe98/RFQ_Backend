package com.kingdom_bank.RFQBackend.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SoaGetStrongerWeakerDto {
    private String exchangeRate;
    private String convertedAmount;
    private String multiplyDivide;
    private String fromCurrency;
    private String toCurrency;
}
