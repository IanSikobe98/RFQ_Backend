package com.kingdom_bank.RFQBackend.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EntityActions {
    CREATE("CREATE"),
    EDIT("EDIT"),
    CHANGE_STATUS("CHANGESTATUS");

    private final String value;
}
