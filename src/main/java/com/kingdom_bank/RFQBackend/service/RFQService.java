package com.kingdom_bank.RFQBackend.service;


import com.kingdom_bank.RFQBackend.dto.*;
import com.kingdom_bank.RFQBackend.enums.AccountAction;
import com.kingdom_bank.RFQBackend.enums.ApiResponseCode;
import com.kingdom_bank.RFQBackend.enums.CurrencyAction;
import com.kingdom_bank.RFQBackend.enums.IdentificationOptions;
import com.kingdom_bank.RFQBackend.service.soa.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RFQService {
    private final AccountDetailsService accountDetailsService;
    private final GetCifClientService getCifClientService;
    private final GetCustomerAccounts getCustomerAccounts;
    private final DetermineStrongerWeakerCurrencyClient determineStrongerWeakerCurrencyClient;
    private final GetExchangeRateClient getExchangeRateClient;

    public ApiResponse getCustomerAccounts(CustomerRequestDTO request,HttpServletResponse httpServletResponse){
        ApiResponse response = new ApiResponse();
        AccountDetailsResponse accountDetailsResponse = new AccountDetailsResponse();
        try{
            if(request.isCustomer()){
                if(request.getOption().equals(IdentificationOptions.ACCNO)){
                    accountDetailsResponse = accountDetailsService.getAccountDetails(request.getIdentificationNumber());
                }
                else{
                    accountDetailsResponse  = getCifClientService.getCustomerCif(String.valueOf(request.getOption()),request.getIdentificationNumber());
                }


                if(accountDetailsResponse != null && accountDetailsResponse.getResponseCode().equalsIgnoreCase(ApiResponseCode.SUCCESS.getCode())){

                    //If account is for a teller
                    if(accountDetailsResponse.getAccountDetails().getProductId()!=null && accountDetailsResponse.getAccountDetails().getProductId().equalsIgnoreCase("OAB")){
                        CustomerAccountSummary accountSummary = new CustomerAccountSummary();
                        accountSummary.setFullName(accountDetailsResponse.getAccountDetails().getAccountName());

                        CustomerAccount account = new CustomerAccount();
                        account.setAccountNumber(request.getIdentificationNumber());
                        account.setCurrency(accountDetailsResponse.getAccountDetails().getCurrencyCode());
                        account.setAccountName(accountDetailsResponse.getAccountDetails().getAccountName());
                        account.setAccountType(accountDetailsResponse.getAccountDetails().getProductName());
                        account.setAccountType(accountDetailsResponse.getAccountDetails().getProductContextCode());
                        account.setIsOfficeAccount(true);
                        List<CustomerAccount> accounts = new ArrayList<>();
                        accounts.add(account);
                        accountSummary.setAccounts(accounts);

                        log.info("Office Account Fetched succesfully for account, " + request.getIdentificationNumber());
                        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                        response.setResponseCode(ApiResponseCode.SUCCESS);
                        response.setResponseMessage("Accounts successfully fetched");
                        response.setEntity(accountSummary);
                        return response;
                    }


                    //Use Cif to fetch accounts
                    if(accountDetailsResponse.getAccountDetails().getCustomerCode()!=null) {
                        String cifId = accountDetailsResponse.getAccountDetails().getCustomerCode();
                        log.info("Cif for customer {} found successfully CIF: {}", request.getIdentificationNumber(), cifId);

                        CustomerAccountsResponse  customerAccountsResponse = getCustomerAccounts.getCustomerAccounts(cifId);
                        if(customerAccountsResponse.getResponseCode().equalsIgnoreCase(ApiResponseCode.SUCCESS.getCode())){
                            log.info("Accounts Fetched succesfully for account, " + request.getIdentificationNumber());
                            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                            response.setResponseCode(ApiResponseCode.SUCCESS);
                            response.setResponseMessage("Accounts successfully fetched");
                            response.setEntity(customerAccountsResponse.getCustomerAccountSummary());
                            return response;

                        }
                        else{
                            log.error("Error occured fetching customer accounts");
                            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                            response.setResponseCode(ApiResponseCode.FAIL);
                            response.setResponseMessage("Sorry,Error occurred while fetching customer accounts");
                            return  response;
                        }

                    }
                    else{
                        log.error("Error occured fetching id for the customer");
                        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                        response.setResponseCode(ApiResponseCode.FAIL);
                        response.setResponseMessage("Sorry,Error occurred while fetching customer accounts");
                        return  response;
                    }


                }
                else{
                    log.error("Error occured fetching id for the customer");
                    httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                    response.setResponseCode(ApiResponseCode.FAIL);
                    response.setResponseMessage("Sorry,Error occurred while fetching customer accounts");
                    return  response;
                }
            }
            //If Non customer
            else {
                accountDetailsResponse = accountDetailsService.getAccountDetails(request.getIdentificationNumber());
                if(accountDetailsResponse != null && accountDetailsResponse.getResponseCode().equalsIgnoreCase(ApiResponseCode.SUCCESS.getCode())){
                    //If account is for a teller
                    if(accountDetailsResponse.getAccountDetails().getProductId()!=null && accountDetailsResponse.getAccountDetails().getProductId().equalsIgnoreCase("OAB")){
                        CustomerAccountSummary accountSummary = new CustomerAccountSummary();
                        accountSummary.setFullName(accountDetailsResponse.getAccountDetails().getAccountName());

                        CustomerAccount account = new CustomerAccount();
                        account.setAccountNumber(request.getIdentificationNumber());
                        account.setCurrency(accountDetailsResponse.getAccountDetails().getCurrencyCode());
                        account.setAccountName(accountDetailsResponse.getAccountDetails().getAccountName());
                        account.setAccountType(accountDetailsResponse.getAccountDetails().getProductName());
                        account.setAccountType(accountDetailsResponse.getAccountDetails().getProductContextCode());
                        account.setIsOfficeAccount(true);
                        List<CustomerAccount> accounts = new ArrayList<>();
                        accounts.add(account);
                        accountSummary.setAccounts(accounts);

                        log.info("Office Account Fetched succesfully for account, " + request.getIdentificationNumber());
                        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                        response.setResponseCode(ApiResponseCode.SUCCESS);
                        response.setResponseMessage("Accounts successfully fetched");
                        response.setEntity(accountSummary);
                        return response;
                    }
                    else{
                        log.error("Account selected is not a teller account");
                        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                        response.setResponseCode(ApiResponseCode.FAIL);
                        response.setResponseMessage("Sorry,Account selected is not a teller account");
                        return  response;
                    }
                }
                else{
                    log.error("Error occured fetching fetching customer accounts");
                    httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                    response.setResponseCode(ApiResponseCode.FAIL);
                    response.setResponseMessage("Sorry,Error occurred while fetching customer accounts");
                    return  response;
                }
            }

        }
        catch(Exception e){
            log.error("ERROR OCCURRED DURING FETCHING OF CUSTOMER ACCOUNTS {}: {}" ,request,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching customer accounts");
            return  response;
        }

    }


    public ApiResponse getCurrencyDirection(GetCurrencyDirectionDto dto,HttpServletResponse httpServletResponse) {
        ApiResponse response = new ApiResponse();
        try {
            CurrencyAction currencyAction = determineCurrencyActionExplicitViaSoa(dto.getFromCurrency(), dto.getToCurrency());
            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Currency Action Determined Successfully");
            response.setEntity(currencyAction);
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            log.info("Currency Action Determined Successfully");
            return response;

        } catch (Exception e) {
            log.error("Error Determining currencyAction: {}", e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Error Determining currencyAction: " + e.getMessage());
            return response;
        }
    }

    public CurrencyAction determineCurrencyActionExplicitViaSoa(String fromCurrency, String toCurrency) {
        DealtCounterCurrencyDto response = computeDealtAndCounterCurrencyViaSoa(fromCurrency,toCurrency, "1000");
        String strongerCurrency = response.getDealtCurrency();
        String weakerCurrency = response.getCounterCurrency();


        System.out.println("fromCurrency: " + fromCurrency);
        System.out.println("toCurrency: " + toCurrency);

        System.out.println("strongerCurrency: " + strongerCurrency);
        System.out.println("weakerCurrency: " + weakerCurrency);


        System.out.println("toCurrency.equals(strongerCurrency: SELL" + toCurrency.equals(strongerCurrency));
        System.out.println("fromCurrency.equals(weakerCurrency : SELL" + fromCurrency.equals(weakerCurrency));

        System.out.println("toCurrency.equals(weakerCurrency: BUY " + toCurrency.equals(weakerCurrency));
        System.out.println("fromCurrency.equals(strongerCurrency BUY" + fromCurrency.equals(weakerCurrency));

        // Business Rules Implementation:
        // Rule 1: IF TO_CURRENCY IS STRONGER, THE BANK IS SELLING
        // Rule 2: IF FROM_CURRENCY IS WEAKER, THE BANK IS SELLING
        if (toCurrency.equals(strongerCurrency) || fromCurrency.equals(weakerCurrency)) {
            return CurrencyAction.Sell;
        }

        // Rule 3: IF TO_CURRENCY IS WEAKER, THE BANK IS BUYING
        // Rule 4: IF FROM_CURRENCY IS STRONGER, THE BANK IS BUYING
        if (toCurrency.equals(weakerCurrency) || fromCurrency.equals(strongerCurrency)) {
            return CurrencyAction.Buy;
        }
        throw new RuntimeException("Currency action could not be determined for pair: " + fromCurrency + "/" + toCurrency);
    }

    public DealtCounterCurrencyDto computeDealtAndCounterCurrencyViaSoa(String fromCurrency, String toCurrency, String transactionAmount) {
        SOAResponse soaResponse = determineStrongerWeakerCurrencyClient.getExchangeRate(fromCurrency, toCurrency, transactionAmount);

        if (ApiResponseCode.SUCCESS.getCode().equals(soaResponse.getResponseCode())) {
            SoaGetStrongerWeakerDto response = (SoaGetStrongerWeakerDto) soaResponse.getData();
            String soaFromCurrency = response.getFromCurrency();
            String soaToCurrency = response.getToCurrency();
            String multiplyDivide = response.getMultiplyDivide();

            DealtCounterCurrencyDto result = new DealtCounterCurrencyDto();
            if (multiplyDivide.equalsIgnoreCase("M")) {
                //if M, then fromCurrency is stronger and toCurrency is weaker
                result.dealtCurrency = soaFromCurrency;  //Stronger Currency
                result.counterCurrency = soaToCurrency; //Weaker Currency
            } else if (multiplyDivide.equalsIgnoreCase("D")) {
                //if D, then fromCurrency is weaker and toCurrency is Stronger
                result.dealtCurrency = soaToCurrency;  //Stronger Currency
                result.counterCurrency = soaFromCurrency; //Weaker Currency
            }

            System.out.println("Result for computeDealtAndCounterCurrencyViaSoa " + result);

            return result;
        } else {
            throw new RuntimeException("Failed to get exchange rate from SOA service. Response code: " +
                    soaResponse.getResponseCode());
        }
    }


    public ApiResponse getSinglePairExchangeRate(ExchangeRequest body,HttpServletResponse httpServletResponse) {
        System.out.println("body for getting getSinglePairExchangeRate"+  body);
        ApiResponse response = new ApiResponse();

        // Validate input
        if (body.getFromCurrency() == null || body.getToCurrency() == null) {
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("From currency and to currency are required");
            log.info("From currency and to currency are required");
            return response;
        }

        try {
            SOAResponse soaResponse = getExchangeRateClient.getExchangeRate(body);

            if (ApiResponseCode.SUCCESS.getCode().equals(soaResponse.getResponseCode())) {
                log.info("Exchange rates fetched successfully for {} to {}",
                        body.getFromCurrency(), body.getToCurrency());

                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                response.setResponseCode(ApiResponseCode.SUCCESS);
                response.setResponseMessage(soaResponse.getMessage());
                response.setEntity(soaResponse.getData());

            } else {
                log.error("Failed to get exchange rates from {} to {}. Error: {}",
                        body.getFromCurrency(), body.getToCurrency(), soaResponse.getMessage());

                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage(soaResponse.getMessage() != null ?
                        soaResponse.getMessage() :
                        "Unable to fetch exchange rates at this time");
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters for exchange rate from {} to {}: {}",
                    body.getFromCurrency(), body.getToCurrency(), e.getMessage());

            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Invalid request parameters: " + e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Service temporarily unavailable. Please try again later.");
            log.error("Unexpected error getting exchange rates from {} to {}: {}",
                    body.getFromCurrency(), body.getToCurrency(), e.getMessage(), e);
        }

        return response;
    }

}
