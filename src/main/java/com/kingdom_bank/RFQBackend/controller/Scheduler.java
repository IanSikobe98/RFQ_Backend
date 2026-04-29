package com.kingdom_bank.RFQBackend.controller;

import com.kingdom_bank.RFQBackend.dto.ApiResponse;
import com.kingdom_bank.RFQBackend.dto.DealCodeRetryRequest;
import com.kingdom_bank.RFQBackend.entity.AvailabilityConfiguration;
import com.kingdom_bank.RFQBackend.entity.Order;
import com.kingdom_bank.RFQBackend.entity.Status;
import com.kingdom_bank.RFQBackend.repository.AvailabilityConfigRepo;
import com.kingdom_bank.RFQBackend.repository.OrderRepository;
import com.kingdom_bank.RFQBackend.service.RFQService;
import com.kingdom_bank.RFQBackend.service.ScheduledService;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class Scheduler {

    private final Environment environment;
    private final AvailabilityConfigRepo availabilityConfigRepo;
    private final ConstantUtil constantUtil;
    private final ScheduledService scheduledService;
    private final OrderRepository orderRepository;
    private final RFQService rFQService;

    @Scheduled(fixedDelayString = "${autoExpireOrders.fixedDelay}")
    private void autoExpireTransactions() {
        if (environment.getProperty("autoExpireOrders.activate", "1").equalsIgnoreCase("1")) {
            try{
                String eodWeekendDefault = environment.getProperty("eod.weekends","13:00");
                String eodWeekdayDefault =environment.getProperty("eod.weekdays","17:00");
                String eodTimeDefault ="";
                LocalTime eodTime = LocalTime.now();
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE");
                String day = now.format(dayFormatter);
                if(day.equalsIgnoreCase("Saturday")||day.equalsIgnoreCase("Sunday")){
                    eodTimeDefault = eodWeekendDefault;
                }
                else{
                    eodTimeDefault = eodWeekdayDefault;
                }
                eodTime = LocalTime.parse(eodTimeDefault, DateTimeFormatter.ofPattern("HH:mm"));

                //fetch time configuration
                Optional<AvailabilityConfiguration> configuration = availabilityConfigRepo.findByDayAndStatus(day,constantUtil.ACTIVE);
               if(configuration.isPresent()){
                   AvailabilityConfiguration availabilityConfiguration = configuration.get();
                   eodTime = availabilityConfiguration.getEndTime();
               }

               if(eodTime.isBefore(LocalTime.now())) {
                   log.info("eod time  has reached,beginning auto expiry process....");
                   List<Status> statuses = Arrays.asList(constantUtil.PENDING, constantUtil.PENDING_APPROVAL, constantUtil.PENDING_TELLER_APPROVAL,
                           constantUtil.PENDING_DEALER_APPROVAL, constantUtil.PENDING_NEGOTIATION, constantUtil.PROCESSING);
                   List<Order> pendingOrders = orderRepository.findByStatusIn(statuses);
                   if (!pendingOrders.isEmpty()) {
                       log.info(" {} Pending orders found,",pendingOrders.size());
                       for (Order pendingOrder : pendingOrders) {
                           scheduledService.expirePendingTransactions(pendingOrder);
                       }

                   }
               }
            }
            catch (Exception e){
                e.printStackTrace();
                log.error("error occured on Order expiry {}",e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelayString = "${autoRetryOrders.fixedDelay}")
    private void autoRetryFailedTransactions() {
        if (environment.getProperty("autoRetryOrders.activate", "1").equalsIgnoreCase("1")) {
            try {
                List<Status> statuses = Arrays.asList(constantUtil.FAILED);
                List<Order> pendingOrders = orderRepository.findByStatusIn(statuses);
                if(!pendingOrders.isEmpty()){
                    for(Order pendingOrder : pendingOrders) {
                        log.info("order of id picked for retry {}",pendingOrder.getId());
                        DealCodeRetryRequest request = new DealCodeRetryRequest();
                        request.setId(pendingOrder.getId());
                        ApiResponse apiResponse = rFQService.retryPostDealCode(request);
                        log.info("response : {}",apiResponse);
                    }
                }


            } catch (Exception e) {
                e.printStackTrace();
                log.error("error occured on Order expiry {}", e.getMessage());
            }
        }
    }
}
