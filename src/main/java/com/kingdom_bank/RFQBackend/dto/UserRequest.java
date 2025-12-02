package com.kingdom_bank.RFQBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    private String userName;
    private String phoneNumber;
    private String email;
    private Integer status;
    private String statusName;
    private Integer id;
    private Integer roleId;
    private String roleName;
    private String comment;


}
