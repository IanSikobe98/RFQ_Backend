package com.kingdom_bank.RFQBackend.dto;


import com.kingdom_bank.RFQBackend.entity.User;
import lombok.Data;

import java.util.List;

@Data
public class UserInfoDTO {
    private User user;
    private List<String> usersPerm;
    private String role;
}
