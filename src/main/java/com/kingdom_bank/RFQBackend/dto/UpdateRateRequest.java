package com.kingdom_bank.RFQBackend.dto;


import lombok.Data;

@Data
public class UpdateRateRequest {
    private Integer orderId;
    private String rate;
    private String comment;

}
