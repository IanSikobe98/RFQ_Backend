package com.kingdom_bank.RFQBackend.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class SOAResponse<T> {
    private String responseCode;
    private String message;
    private T data;
}
