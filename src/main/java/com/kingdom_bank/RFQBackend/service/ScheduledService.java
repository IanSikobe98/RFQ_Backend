package com.kingdom_bank.RFQBackend.service;

import com.kingdom_bank.RFQBackend.entity.Order;
import com.kingdom_bank.RFQBackend.repository.OrderRepository;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScheduledService {

    private final OrderRepository orderRepository;
    private final ConstantUtil constantUtil;


    public void expirePendingTransactions(Order pendingOrder){
        try{
            pendingOrder.setStatus(constantUtil.EXPIRED);
            pendingOrder.setUpdatedBy("SYSTEM");
            orderRepository.saveAndFlush(pendingOrder);
            log.info("Order {} successfully expired ", pendingOrder.getId());
        }
        catch (Exception e){
            log.error("error occurred while expiring transactions: {}" ,e.getMessage());
            e.printStackTrace();
        }
    }
}
