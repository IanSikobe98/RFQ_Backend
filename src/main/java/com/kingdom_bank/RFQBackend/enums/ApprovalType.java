package com.kingdom_bank.RFQBackend.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalType {
    ROLE("ROLE"),
    ROLE_UPDATE("ROLE_UPDATE"),
    USER("USER"),
    USER_UPDATE("USER_UPDATE")
    ;
    private final String value;
}
