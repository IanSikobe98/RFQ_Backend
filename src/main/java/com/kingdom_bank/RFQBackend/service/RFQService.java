package com.kingdom_bank.RFQBackend.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kingdom_bank.RFQBackend.config.security.SecurityUser;
import com.kingdom_bank.RFQBackend.dto.*;
import com.kingdom_bank.RFQBackend.entity.*;
import com.kingdom_bank.RFQBackend.enums.*;
import com.kingdom_bank.RFQBackend.repository.ApprovedDealsRepo;
import com.kingdom_bank.RFQBackend.repository.OrderRepository;
import com.kingdom_bank.RFQBackend.service.soa.*;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static com.kingdom_bank.RFQBackend.enums.Action.APPROVE;
import static com.kingdom_bank.RFQBackend.enums.Action.REJECT;
import static com.kingdom_bank.RFQBackend.util.CommonTasks.generateDealCode;
import static com.kingdom_bank.RFQBackend.util.CommonTasks.generateOrderId;

@Service
@RequiredArgsConstructor
@Slf4j
public class RFQService {
    private final AccountDetailsService accountDetailsService;
    private final GetCifClientService getCifClientService;
    private final GetCustomerAccounts getCustomerAccounts;
    private final DetermineStrongerWeakerCurrencyClient determineStrongerWeakerCurrencyClient;
    private final GetExchangeRateClient getExchangeRateClient;
    private final OrderRepository orderRepository;
    private final ConstantUtil constantUtil;
    private final ApprovedDealsRepo approvedDealsRepo;

    @Value("${rfq.duplication.threshhold}")
    private String duplicationThreshold;

    @Value("${params.admin_role}")
    private String adminRole;

    /**
     * Function to get the Authenticated user that was authenticated using JWT
     * @return ApiUser: The authenticated user
     */
    private User getauthenticatedAPIUser(){
        return  ((SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
    }

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
                        account.setBranchCode(accountDetailsResponse.getAccountDetails().getBranchCode());
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

    public ApiResponse createRFQ(CreateRFQRequest request,HttpServletResponse httpServletResponse) {
        ApiResponse response = new ApiResponse();
        try{

            User user = getauthenticatedAPIUser();
            log.info("Starting duplicate check for customer: {}, account: {}, amount: {}",
                    request.getCustomerNo(), request.getAccountNumber(), request.getAmount());

            LocalDateTime timeThreshold = LocalDateTime.now().minusMinutes(Long.parseLong(duplicationThreshold));

            List<Order> orders = orderRepository.findRecentDuplicateRFQs(request.getCustomerNo(),request.getAccountNumber(),request.getFromCurrency(),
                    request.getToCurrency(),request.getAmount(),timeThreshold,constantUtil.PENDING_APPROVAL);
            if(!orders.isEmpty()){
                Order order = orders.getFirst();
                log.info("Duplicate order {} found with status {}", order.getOrderId(),constantUtil.PENDING_APPROVAL);
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Duplicate order found");
                return  response;
            }

            CurrencyAction currencyAction = determineCurrencyActionExplicitViaSoa(
                    request.getFromCurrency(), request.getToCurrency());


            Order order = Order.builder()
                    .orderId(generateOrderId(request.getAccountNumber()))
                    .accountNumber(request.getAccountNumber())
                    .customerName(request.getCustomerName())
                    .tellerCashAccountName(request.getTellerAccountName())

                    .cifAccountCode(request.getCustomerNo())

                    .counterNominalAmount(request.getAmount())
                    .currencyPair(request.getFromCurrency()+"/"+request.getToCurrency())
                    .fromCurrency(request.getFromCurrency())
                    .toCurrency(request.getToCurrency())
                    .buySell(currencyAction.name().toUpperCase())
                    .treasuryRate(new BigDecimal(request.getTreasuryRate()))

                    .purpose(request.getPurpose())
                    .requestDate(new Date())
                    .valueDate(request.getValueDate())

                    .comments(request.getComments())
                    .expectedAmount(request.getAmount().multiply(new BigDecimal(request.getNegotiatedRate())))

                    .branchId(request.getBranchCode())
                    .tellerId(user.getUsername())

                    .negotiatedRate(new BigDecimal(request.getNegotiatedRate()))
//                    .validUntil(new Date())

                    .createdBy(user.getUsername())
                    .dateAdded(new Date())
                    .status(constantUtil.PENDING_APPROVAL)

                    .build();

            orderRepository.saveAndFlush(order);
            log.info("Created order with status successfully: {}", order.getOrderId());

            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Deal Request successfullly Submitted");

        }
        catch (Exception e) {
        e.printStackTrace();
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        response.setResponseCode(ApiResponseCode.FAIL);
        response.setResponseMessage("Error occurred while creating RFQ");
        log.error("Unexpected error when creating RFQ  for request {} {}", request,e.getMessage(), e);
    }
        return  response;
}



    public ReportResponse getDealRequests(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<Order> dealRequestsList = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();

            if (request.getStatuses() != null  && !request.getStatuses().isEmpty()) {
                dealRequestsList = orderRepository.findByStatus_StatusIdInOrderByDateAddedDesc(request.getStatuses());
            } else {
                dealRequestsList = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "dateAdded"));
            }


            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Deal Requests successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(dealRequestsList), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING DEAL REQUESTS DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the users Data");
        }
        return response;

    }


    public ApiResponse approveOrRejectDealRequests(ApprovalRequest request, User loggedInUser, Integer id){
        ApiResponse response = new ApiResponse();
        log.info("Approving user of id {}...",id);

        try {
            Optional<Order> existingOrderOptional = orderRepository.findById(id);
            if (!existingOrderOptional.isPresent()) {
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("User  with id  "+ id+ " does not exist");
                return response;
            }
            Order existingOrder = existingOrderOptional.get();

            String userRole = loggedInUser.getRole().getRoleName();
            if(!userRole.equalsIgnoreCase(adminRole)) {
                if (existingOrder.getCreatedBy().equalsIgnoreCase(loggedInUser.getUsername())) {
                    response.setResponseCode(ApiResponseCode.FAIL);
                    response.setResponseMessage("User cannot approve the order it created");
                    return response;
                }
            }

            UserRequest userRequest = UserRequest.builder().id(id).build();
            userRequest.setComment(request.getDescription());
            if(request.getAction().equals(APPROVE.getValue())){




                existingOrder.setDateApproved(new Date());
                existingOrder.setApprovedBy(loggedInUser.getUsername());
                existingOrder.setStatus(constantUtil.ACTIVE);
                existingOrder.setDealerCode(generateDealCode(existingOrder.getFromCurrency(),existingOrder.getToCurrency(),existingOrder.getValueDate(),existingOrder.getId()));
                existingOrder.setDealerId(loggedInUser.getUsername());
                orderRepository.save(existingOrder);

                //TODO CONFIRM FIELDS FOR APPROVAL I.E SOLD AMOUNT BOUGHT AMOUNT ETX


                //Stage approved deal in new table
                ApprovedDeals approvedDeal = ApprovedDeals.builder()
                        .status(constantUtil.ACTIVE)
                        .order(existingOrder)
                        .orderCode(existingOrder.getOrderId())
                        .orderStatus(constantUtil.ACTIVE)
                        .boughtCurrency(existingOrder.getFromCurrency())
                        .soldCurrency(existingOrder.getToCurrency())
                        .exchangeRate(existingOrder.getNegotiatedRate())
                        .treasuryRate(existingOrder.getTreasuryRate())
                        .dealerCode(existingOrder.getDealerCode())

                        .cifAccountCode(existingOrder.getCifAccountCode())
                        .valueDate(existingOrder.getValueDate())
                        .accountNumber(existingOrder.getAccountNumber())
                        .createdBy(existingOrder.getCreatedBy())
                        // dateAdded will be set automatically by DB (CURRENT_TIMESTAMP)
                        .build();


                if(existingOrder.getBuySell().equalsIgnoreCase("BUY")){
                    approvedDeal.setBoughtAmount(existingOrder.getCounterNominalAmount());
                }
                else if(existingOrder.getBuySell().equalsIgnoreCase("SELL")){
                    approvedDeal.setSoldAmount(existingOrder.getCounterNominalAmount());
                }

                approvedDealsRepo.save(approvedDeal);
                log.info("Approved Deals staged successfully , {}",approvedDeal);



                log.info("Deal Order Request {}  successfully  approved",existingOrder.getId());
                response.setResponseMessage("Deal Request successfully Approved.");

                response.setResponseCode(ApiResponseCode.SUCCESS);
            }
            else if(request.getAction().equals(REJECT.getValue())){
                existingOrder.setDateApproved(new Date());
                existingOrder.setApprovedBy(loggedInUser.getUsername());
                existingOrder.setStatus(constantUtil.REJECTED);
                orderRepository.save(existingOrder);

                log.info("Order {} successfully  rejected",existingOrder.getId());
                response.setResponseMessage("Order record successfully Rejected.");
                response.setResponseCode(ApiResponseCode.SUCCESS);
            }
            else{
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("approval action is invalid");
                return response;
            }
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING APPROVAL OF ORDER: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred during approval of order");
        }
        return response;
    }



}
