package com.kingdom_bank.RFQBackend.util;

import com.kingdom_bank.RFQBackend.entity.Role;
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
    public Status PENDING_APPROVAL;
    public Status PENDING_TELLER_APPROVAL;
    public Status PENDING_DEALER_APPROVAL;
    public Status REJECTED;
    public Status PENDING_NEGOTIATION;
    public Status PROCESSING;
    public Status NEW;
    public Status NOTIFICATION_SENT;

    public Role TREASURY_DEALER;

    @PostConstruct
    public void init() {
        INACTIVE = commonTasks.getStatus(0);
        ACTIVE = commonTasks.getStatus(1);
        PENDING = commonTasks.getStatus(2);
        FAILED = commonTasks.getStatus(3);
        SUCCESS = commonTasks.getStatus(4);
        OTP_SENT = commonTasks.getStatus(5);
        PENDING_APPROVAL = commonTasks.getStatus(6);
        REJECTED = commonTasks.getStatus(7);
        PENDING_TELLER_APPROVAL = commonTasks.getStatus(8);
        PENDING_DEALER_APPROVAL = commonTasks.getStatus(9);
        PENDING_NEGOTIATION = commonTasks.getStatus(10);
        PROCESSING = commonTasks.getStatus(11);
        NEW = commonTasks.getStatus(12);
        NOTIFICATION_SENT = commonTasks.getStatus(13);


        TREASURY_DEALER = commonTasks.getRole(1006);

    }
}
