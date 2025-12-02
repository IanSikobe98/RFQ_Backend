package com.kingdom_bank.RFQBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class PageInfo {
    private int number;
    private int size;
    private int totalPages;
    private Long totalElements;

    public PageInfo() {
        this.number = 0;
        this.size = 0;
        this.totalElements = 0L;
        this.totalPages = 0;
    }



}
