package com.kingdom_bank.RFQBackend.service;

import com.kingdom_bank.RFQBackend.entity.Status;
import com.kingdom_bank.RFQBackend.entity.User;
import com.kingdom_bank.RFQBackend.repository.UserRepo;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;
    private final ConstantUtil constantUtil;

    public User getUserCredsByUsername(String username) {
        List<Status> statuses = Arrays.asList(constantUtil.ACTIVE);
        return userRepo.findDistinctByUsernameEqualsIgnoreCaseAndStatusIn(username,statuses);
    }
}
