package com.kingdom_bank.RFQBackend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SanctionVerificationRequest {
    private String firstName;
    private String middleName;
    private String lastName;
    private String documentType;
    private String docNumber;
    private String dob;
}
