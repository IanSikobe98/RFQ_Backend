package com.kingdom_bank.RFQBackend.util;

import com.kingdom_bank.RFQBackend.entity.Status;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConstantUtil {
    private final CommonTasks commonTasks;





    public Status INACTIVE;
    public Status ACTIVE;
    public Status PENDING;
    public Status FAILED;
    public Status SUCCESS;
    public Status OTP_SENT;

    @PostConstruct
    public void init() {
        INACTIVE = commonTasks.getStatus(0);
        ACTIVE = commonTasks.getStatus(1);
        PENDING = commonTasks.getStatus(2);
        FAILED = commonTasks.getStatus(3);
        SUCCESS = commonTasks.getStatus(4);
        OTP_SENT = commonTasks.getStatus(5);
    }
}
