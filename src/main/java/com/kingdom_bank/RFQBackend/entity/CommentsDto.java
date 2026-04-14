package com.kingdom_bank.RFQBackend.entity;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class CommentsDto {
    private String comment;
    private Long id;
    private Date dateCreated;
    private Integer createdBy;
    private String creator;


}
