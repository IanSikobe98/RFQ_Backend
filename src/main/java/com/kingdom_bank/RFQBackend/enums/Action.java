package com.kingdom_bank.RFQBackend.enums;
import lombok.AllArgsConstructor;
import lombok.Getter;
@Getter
@AllArgsConstructor
public enum Action {
    APPROVE("APPROVE"),
    REJECT("REJECT"),
    NEGOTIATE("NEGOTIATE");
    private final String value;
}
