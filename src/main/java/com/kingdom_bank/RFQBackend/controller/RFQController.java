package com.kingdom_bank.RFQBackend.controller;

import com.google.gson.Gson;
import com.kingdom_bank.RFQBackend.dto.*;
import com.kingdom_bank.RFQBackend.service.RFQService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rfq")
@RequiredArgsConstructor
@Slf4j
public class RFQController {

    private final RFQService rFQService;

    @PostMapping("/fetchAccounts")
    public ApiResponse fetchAccounts(HttpServletResponse httpServletResponse , @RequestBody @Valid CustomerRequestDTO request){
        log.info("FETCH ACCOUNTS REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = rFQService.getCustomerAccounts(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/getCurrencyDirection")
    public ApiResponse getCurrencyDirection(HttpServletResponse httpServletResponse , @RequestBody @Valid GetCurrencyDirectionDto request){
        log.info("GET CURRENCY DIRECTION REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = rFQService.getCurrencyDirection(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/getSinglePairExchangeRate")
    public ApiResponse getSinglePairExchangeRate(HttpServletResponse httpServletResponse , @RequestBody @Valid ExchangeRequest request){
        log.info("GET SINGLE PAIR EXCHANGE REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = rFQService.getSinglePairExchangeRate(request ,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/createRFQ")
    public ApiResponse createRFQ(HttpServletResponse httpServletResponse , @RequestBody @Valid CreateRFQRequest request){
        log.info("GET CREATE RFQ REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = rFQService.createRFQ(request ,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/getDealRequests")
    public ApiResponse fetchDealRequests(HttpServletResponse httpServletResponse , @RequestBody @Valid ReportRequest request){
        log.info("GET fetch Deal Requests REQUEST :: {}", new Gson().toJson(request));
        ReportResponse response = rFQService.getDealRequests(request ,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }


}
