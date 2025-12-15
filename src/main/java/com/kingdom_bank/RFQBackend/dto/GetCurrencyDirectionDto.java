package com.kingdom_bank.RFQBackend.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class GetCurrencyDirectionDto {
    String fromCurrency;
    String toCurrency;
}
