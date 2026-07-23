package com.kingdom_bank.RFQBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchRequest {
    private String bankCode;
    private String branchCode;
    private String branchName;
    private Integer id;
    private String comment;
}
