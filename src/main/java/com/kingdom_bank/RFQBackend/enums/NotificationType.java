package com.kingdom_bank.RFQBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum NotificationType {
    CREATE_RFQ("NEW DEAL REQUEST"),
    DEALER_RATE("QUOTE READY FOR ACTION"),
    RFQ_APPROVED("DEAL ACCEPTANCE"),
    RFQ_REJECTED("DEAL REJECTION"),
    NEGOTIATE_RATE("DEAL NEGOTIATION");

    private final String message;
    NotificationType(String message){this.message = message;}




    }
