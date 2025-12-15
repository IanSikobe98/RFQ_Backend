package com.kingdom_bank.RFQBackend.dto;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DealtCounterCurrencyDto {
    public String dealtCurrency;
    public String counterCurrency;
}
