package com.kingdom_bank.RFQBackend.controller;

import com.google.gson.Gson;
import com.kingdom_bank.RFQBackend.dto.*;
import com.kingdom_bank.RFQBackend.service.RFQService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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


    @PostMapping("/updateRate")
    public ApiResponse updateRate(HttpServletResponse httpServletResponse , @RequestBody @Valid UpdateRateRequest request){
        log.info("GET UPDATE RATE REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = rFQService.updateRate(request ,httpServletResponse);
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

    @PostMapping("/fetchAccountDetails/{accountNumber}")
    public ApiResponse fetchAccountDetails(HttpServletResponse httpServletResponse , @PathVariable("accountNumber") String accountNumber){
        log.info("GET fetch Account Details Requests REQUEST :: {}", accountNumber);
        ApiResponse response = rFQService.fetchAccounts(accountNumber);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/getAllExchangeRates")
    public ApiResponse getAllExchangeRates(HttpServletResponse httpServletResponse){
        log.info("GET All Exchange Rates REQUEST ");
        ApiResponse response = rFQService.getAllExchangeRates();
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/retryPostDealCode")
    public ApiResponse retryPostDealCode(HttpServletResponse httpServletResponse , @RequestBody @Valid DealCodeRetryRequest request){
        log.info("RETRY POST DEAL CODE REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = rFQService.retryPostDealCode(request ,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/validateAmount")
    public ApiResponse validateAmount(HttpServletResponse httpServletResponse , @RequestBody @Valid AmountValidationDTO request){
        log.info("VALIDATE AMOUNT REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = rFQService.validateAmountInput(request ,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/checkAvailability")
    public ApiResponse checkAvailability(HttpServletResponse httpServletResponse){
        log.info("CHECK AVAILABILITY REQUEST :: ");
        ApiResponse response = rFQService.validateAvailabilitySchedule(httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/updateSchedule")
    public ApiResponse updateSchedule(@RequestBody @Valid  UpdateScheduleDTO updateScheduleDTO, HttpServletResponse httpServletResponse){
        log.info("UPDATE AVAILABILITY SCHEDULE REQUEST :: ");
        ApiResponse response = rFQService.updateAvailabilitySchedule(updateScheduleDTO,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/fetchSchedule")
    public ReportResponse fetchSchedule(HttpServletResponse httpServletResponse){
        log.info("FETCH AVAILABILITY SCHEDULE REQUEST :: ");
        ReportResponse response = rFQService.fetchAvailabilitySchedule(httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/fetchAmountConfiguration")
    public ApiResponse fetchAmountConfiguration(HttpServletResponse httpServletResponse){
        log.info("FETCH AMOUNT CONFIGURATION REQUEST :: ");
        ApiResponse response = rFQService.fetchAmountConfiguration(httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/updateAmountConfiguration")
    public ApiResponse updateAmountConfiguration(@RequestBody @Valid UpdateAmountLimitConfigDTO request,HttpServletResponse httpServletResponse){
        log.info("UPDATE AMOUNT CONFIGURATION REQUEST :: ");
        ApiResponse response = rFQService.updateAmountConfiguration(request ,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }


}
