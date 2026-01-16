package com.kingdom_bank.RFQBackend.controller;

import com.kingdom_bank.RFQBackend.dto.ApiResponse;
import com.kingdom_bank.RFQBackend.service.DashboardService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {


    private final DashboardService dashboardService;

    @PostMapping("/fetchDashStats")
    public ApiResponse fetchDashStatus(HttpServletResponse httpServletResponse){
        log.info("FETCH DASH STATS REQUEST ");
        ApiResponse response = dashboardService.getDashStats();
        log.info("FETCH DASH STATS RESPONSE {}", response);
        return response;
    }
}
