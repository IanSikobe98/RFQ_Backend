package com.kingdom_bank.RFQBackend.dto;

import com.kingdom_bank.RFQBackend.enums.ApprovalType;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalRequest {
    private String action;
    private String description;
    private List<String> ids;
    private ApprovalType approvalType;
}