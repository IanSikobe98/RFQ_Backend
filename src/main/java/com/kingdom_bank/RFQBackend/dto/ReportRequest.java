package com.kingdom_bank.RFQBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportRequest {
    private int page;
    private int size;
    private String status;
    private List<Integer> statuses;
    private String id;
    private String type;
    private String name;
    private String query;
    private String dateFrom;
    private String dateTo;
    private Long targetRecordId;
    private String authorizationOperation;
    private String auditEntity;
}
