package com.kingdom_bank.RFQBackend.dto;

import com.kingdom_bank.RFQBackend.enums.IdentificationOptions;
import lombok.Data;

@Data
public class CustomerRequestDTO {
    private IdentificationOptions option;
    private boolean customer;
    private String identificationNumber;
}
