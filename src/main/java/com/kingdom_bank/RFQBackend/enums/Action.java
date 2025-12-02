package com.kingdom_bank.RFQBackend.enums;
import lombok.AllArgsConstructor;
import lombok.Getter;
@Getter
@AllArgsConstructor
public enum Action {
    APPROVE("APPROVE"),
    REJECT("REJECT");
    private final String value;
}
