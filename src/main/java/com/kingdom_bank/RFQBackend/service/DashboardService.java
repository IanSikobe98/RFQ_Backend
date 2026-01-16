package com.kingdom_bank.RFQBackend.service;

import com.kingdom_bank.RFQBackend.dto.ApiResponse;
import com.kingdom_bank.RFQBackend.dto.DashStats;
import com.kingdom_bank.RFQBackend.enums.ApiResponseCode;
import com.kingdom_bank.RFQBackend.repository.OrderRepository;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final ConstantUtil constantUtil;

    public ApiResponse getDashStats(){
        ApiResponse response = new ApiResponse();
        try {
            Double noOfDeals = (double) orderRepository.count();
            Double noOfActiveDeals = orderRepository.countByStatus(constantUtil.ACTIVE);

            Double successRate = (double) (noOfActiveDeals / noOfDeals * 100);

            LocalDate today = LocalDate.now();

            LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
            LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

            ZonedDateTime startDateTime = startOfWeek.atStartOfDay(ZoneId.systemDefault());
            ZonedDateTime endDateTime = endOfWeek.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault());

            Date startDate = Date.from(startDateTime.toInstant());
            Date endDate = Date.from(endDateTime.toInstant());

            Double weekDeals = (double) orderRepository.findByDateApprovedBetweenAndStatus(startDate, endDate, constantUtil.ACTIVE).size();

            Double weekRate = weekDeals / noOfActiveDeals * 100;
            DashStats dashStats = DashStats.builder()
                    .activeDeals(noOfActiveDeals)
                    .successRate(successRate)
                    .weekRate(weekRate)
                    .build();

            response.setEntity(dashStats);
            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Dashboard Statistics successfully fetched");
            log.info("DashStats successfully fetched: {}", dashStats);
        }
        catch (Exception e){
            e.printStackTrace();
            log.info(e.getMessage());
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Error occurred while fetching dashboard statistics");
        }
        return response;
    }
}
