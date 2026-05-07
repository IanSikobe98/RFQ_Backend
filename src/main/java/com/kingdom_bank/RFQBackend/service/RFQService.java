package com.kingdom_bank.RFQBackend.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kingdom_bank.RFQBackend.config.security.SecurityUser;
import com.kingdom_bank.RFQBackend.dto.*;
import com.kingdom_bank.RFQBackend.entity.*;
import com.kingdom_bank.RFQBackend.enums.*;
import com.kingdom_bank.RFQBackend.repository.*;
import com.kingdom_bank.RFQBackend.service.soa.*;
import com.kingdom_bank.RFQBackend.util.CommonTasks;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.ldap.server.EmbeddedLdapServerContainer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.kingdom_bank.RFQBackend.enums.Action.*;
import static com.kingdom_bank.RFQBackend.util.CommonTasks.generateOrderId;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RFQService {
    private final AccountDetailsService accountDetailsService;
    private final GetCifClientService getCifClientService;
    private final GetCustomerAccounts getCustomerAccounts;
    private final DetermineStrongerWeakerCurrencyClient determineStrongerWeakerCurrencyClient;
    private final GetExchangeRateClient getExchangeRateClient;
    private final OrderRepository orderRepository;
    private final ConstantUtil constantUtil;
    private final ApprovedDealsRepo approvedDealsRepo;
    private final CommentsRepo commentsRepo;
    private final PostDealCodeService postDealCodeService;
    private final NotificationService notificationService;
    private final UserRepo userRepo;
    private final CommonTasks commonTasks;
    private final ObjectMapper objectMapper;
    private final AvailabilityConfigRepo availabilityConfigRepo;
    private final AmountConfigurationRepo amountConfigurationRepo;
    private final BranchRepo branchRepo;
    private final Environment environment;

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
                            log.error("Error occured fetching customer accounts {}",customerAccountsResponse.getResponseMessage());
                            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                            response.setResponseCode(ApiResponseCode.FAIL);
                            response.setResponseMessage(customerAccountsResponse.getResponseMessage());
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
            throw new RuntimeException("Failed to get exchange rate from service. Response code: " +
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

            ExchangeRequest body = ExchangeRequest.builder()
                    .fromCurrency(request.getFromCurrency())
                    .toCurrency(request.getToCurrency())
                    .account(request.getAccountNumber())
                    .transactionAmount(String.valueOf(request.getAmount()))
                    .build();

            SOAResponse soaResponse = getExchangeRateClient.getExchangeRate(body);

            if (ApiResponseCode.SUCCESS.getCode().equals(soaResponse.getResponseCode())) {
                log.info("Exchange rates fetched successfully for {} to {}",
                        body.getFromCurrency(), body.getToCurrency());

                ObjectMapper mapper = new ObjectMapper();
//                Map<String,Object> responseData = mapper.convertValue(soaResponse.getData(), Map.class);
                Map<String, Object> responseData = (Map<String, Object>) soaResponse.getData();

                BigDecimal treasurycost = responseData.containsKey("treasuryRate")?new BigDecimal(String.valueOf(responseData.get("treasuryRate"))):null;

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
                        .strongCurrency(request.getStrongCurrency())
                        .weakCurrency(request.getWeakCurrency())
                        .amountCurrency(request.getAmountCurrency())
                        .buySell(request.getBankDirection().toUpperCase())
                        .treasuryRate(new BigDecimal(request.getTreasuryRate()))
                        .treasuryCostRate(treasurycost)

                        .purpose(request.getPurpose())
                        .requestDate(new Date())
                        .valueDate(request.getValueDate())

//                    .comments(request.getComments())
//                    .expectedAmount(request.getAmount().multiply(new BigDecimal(request.getNegotiatedRate())))

                        .branchId(user.getBranchId())
                        .tellerId(user.getUsername())

//                    .negotiatedRate(new BigDecimal(request.getNegotiatedRate()))
//                    .validUntil(new Date())

                        .createdBy(user.getUsername())
                        .dateAdded(new Date())
                        .status(constantUtil.PENDING_DEALER_APPROVAL)

                        .build();


//            //Determine currency Action so as to know which is stronger so as to know whether to multiply or divide
//            CurrencyAction currencyAction = determineCurrencyActionExplicitViaSoa(
//                    request.getFromCurrency(), request.getToCurrency());
//
//
//            if(currencyAction.equals(CurrencyAction.Sell)){
//                order.setExpectedAmount(request.getAmount().divide(new BigDecimal(request.getNegotiatedRate()),2,RoundingMode.HALF_UP));
//            }
//            else if(currencyAction.equals(CurrencyAction.Buy)){
//                order.setExpectedAmount(request.getAmount().multiply(new BigDecimal(request.getNegotiatedRate())));
//            }

                orderRepository.saveAndFlush(order);
                log.info("Created order with status successfully: {}", order.getOrderId());



                saveComments(request.getComments(),order,user);
                log.info("Initiating sending of notification......");
                String message = "RFQ OF ORDER "+order.getOrderId()+" successfully created";
                notificationService.sendRoleNotification(message,order,constantUtil.TREASURY_DEALER,NotificationType.CREATE_RFQ);
                log.info("Notification successfully sent to dealer.......");

                response.setResponseCode(ApiResponseCode.SUCCESS);
                response.setResponseMessage("Deal Request successfullly Submitted");

            } else {
                log.error("Failed to get exchange rates from {} to {}. Error: {}",
                        body.getFromCurrency(), body.getToCurrency(), soaResponse.getMessage());

                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage(soaResponse.getMessage() != null ?
                        soaResponse.getMessage() :
                        "Unable to fetch exchange rates at this time");
            }




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

   private void saveComments(String comment,Order order,User user){
       Comments comments = Comments.builder()
               .comment(comment)
               .createdBy(user)
               .updatedBy(user)
               .order(order)
               .build();

       commentsRepo.saveAndFlush(comments);
       log.info("Comments Created Successfully");
    }


    public ApiResponse updateRate(UpdateRateRequest request,HttpServletResponse httpServletResponse){
        ApiResponse response = new ApiResponse();
        try{

            User user = getauthenticatedAPIUser();
            log.info("Updating rate for request : {}",request);

            Optional<Order> orderOptional = orderRepository.findById(Long.valueOf(request.getOrderId()));
            if(orderOptional.isEmpty()){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Order Not Found");
                return response;
            }

            Order order = orderOptional.get();
            order.setNegotiatedRate(new BigDecimal(request.getRate()));
            order.setUpdatedBy(user.getUsername());
            order.setDealerId(user.getUsername());
            order.setExpectedCurrency(request.getExpectedCurrency());
            order.setStatus(constantUtil.PENDING_TELLER_APPROVAL);


//            //Determine currency Action so as to know which is stronger so as to know whether to multiply or divide
//            CurrencyAction currencyAction = determineCurrencyActionExplicitViaSoa(
//                    order.getFromCurrency(), order.getToCurrency());




            if(order.getAmountCurrency().equals(order.getStrongCurrency())){
                order.setExpectedAmount(order.getCounterNominalAmount().multiply(new BigDecimal(request.getRate())));
            }
            else {
                order.setExpectedAmount(order.getCounterNominalAmount().divide(new BigDecimal(request.getRate()),2,RoundingMode.HALF_UP));
            }

            orderRepository.saveAndFlush(order);


            saveComments(request.getComment(),order,user);
            log.info("Initiating sending of notification......");
            String message = "Dealer has updated the rate for ORDER "+order.getOrderId();
            List<Status> statusList = Collections.singletonList(constantUtil.ACTIVE);
            User teller = userRepo.findDistinctByUsernameEqualsIgnoreCaseAndStatusIn(order.getTellerId(),statusList);
            if(teller!=null){
                notificationService.sendUserNotification(message,order,teller,NotificationType.DEALER_RATE);
                log.info("Notification successfully sent to teller {} .......",teller.getUsername());
            }

            response.setResponseMessage("Proposed rate updated successfully");
            response.setResponseCode(ApiResponseCode.SUCCESS);


            log.info("rates updated successfully for request : {}",request);



        }
        catch (Exception e) {
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Error occurred while updating rate");
            log.error("Unexpected error when updating rate  for request {} {}", request,e.getMessage(), e);
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


            dealRequestsList.forEach(order -> {
                List<CommentsDto> commentsDtoList = new ArrayList<>();
                List<Comments> comments = commentsRepo.findByOrder_IdOrderByDateCreatedAsc(order.getId());
                if(!comments.isEmpty()){
                    for(Comments comment :comments) {
                      CommentsDto commentsDto = CommentsDto.builder()
                              .comment(comment.getComment())
                              .dateCreated(comment.getDateCreated())
                              .id(comment.getId())
                              .createdBy(comment.getCreatedBy().getUserId())
                              .creator(comment.getCreatedBy().getUsername())
                              .build();
                      commentsDtoList.add(commentsDto);
                   }
                   order.setCommentsDtoList(commentsDtoList);
               }
            });



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
            Optional<Order> existingOrderOptional = orderRepository.findById(Long.valueOf(id));
            if (!existingOrderOptional.isPresent()) {
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("User  with id  "+ id+ " does not exist");
                return response;
            }
            Order existingOrder = existingOrderOptional.get();

            String userRole = loggedInUser.getRole().getRoleName();
//            if(!userRole.equalsIgnoreCase(adminRole)) {
//                if (existingOrder.getCreatedBy().equalsIgnoreCase(loggedInUser.getUsername())) {
//                    response.setResponseCode(ApiResponseCode.FAIL);
//                    response.setResponseMessage("User cannot approve the order it created");
//                    return response;
//                }
//            }

            UserRequest userRequest = UserRequest.builder().id(id).build();
//            userRequest.setComment(request.getDescription());
            if(request.getAction().equals(APPROVE.getValue())){
                String dealCode = commonTasks.generateDealCode();
                existingOrder.setDealerCode(dealCode);
                existingOrder.setDateApproved(new Date());
                existingOrder.setApprovedBy(loggedInUser.getUsername());


                PostDealCodeResponse postDealCodeResponse = postDealCodeService.postDealCode(existingOrder);

                if(postDealCodeResponse.getResponseCode().equalsIgnoreCase(ApiResponseCode.SUCCESS.getCode())) {


                    updateOrderAndCreateApprovedDeals(existingOrder);


                    log.info("Initiating sending of notification......");
                    String message = "Teller has approved the rate "+String.format("%.2f", existingOrder.getNegotiatedRate())+" for ORDER "+existingOrder.getOrderId();
                    List<Status> statusList = Collections.singletonList(constantUtil.ACTIVE);
                    User dealer = userRepo.findDistinctByUsernameEqualsIgnoreCaseAndStatusIn(existingOrder.getDealerId(),statusList);
                    if(dealer!=null){
                        notificationService.sendUserNotification(message,existingOrder,dealer,NotificationType.RFQ_APPROVED);
                        log.info("Notification successfully sent to dealer {} .......",dealer.getUsername());
                    }

                    log.info("Deal Order Request {}  successfully  approved", existingOrder.getId());
                    response.setResponseMessage("Deal Request successfully Approved.");
                    Map<String, String> dealInfo = new HashMap<>();
                    dealInfo.put("dealCode", dealCode);
                    response.setEntity(dealInfo);
                    response.setResponseCode(ApiResponseCode.SUCCESS);
                }

                else{
                    existingOrder.setStatus(constantUtil.FAILED);
                    orderRepository.save(existingOrder);
                    response.setResponseCode(ApiResponseCode.FAIL);
                    response.setResponseMessage("Failed to post deal code to Finacle");

                }
            }
            else if(request.getAction().equals(REJECT.getValue())){
                existingOrder.setDateApproved(new Date());
                existingOrder.setApprovedBy(loggedInUser.getUsername());
                existingOrder.setStatus(constantUtil.REJECTED);
                orderRepository.save(existingOrder);

                saveComments(request.getDescription(),existingOrder,loggedInUser);
                log.info("Initiating sending of notification......");
                String message = "Teller has rejected rate "+existingOrder.getNegotiatedRate()+" for ORDER "+existingOrder.getOrderId();
                List<Status> statusList = Collections.singletonList(constantUtil.ACTIVE);
                User dealer = userRepo.findDistinctByUsernameEqualsIgnoreCaseAndStatusIn(existingOrder.getDealerId(),statusList);
                if(dealer!=null){
                    notificationService.sendUserNotification(message,existingOrder,dealer,NotificationType.RFQ_REJECTED);
                    log.info("Notification successfully sent to dealer {} .......",dealer.getUsername());
                }


                log.info("Order {} successfully  rejected",existingOrder.getId());
                response.setResponseMessage("Order record successfully Rejected.");
                response.setResponseCode(ApiResponseCode.SUCCESS);
            }
            else if(request.getAction().equals(NEGOTIATE.getValue())){
                existingOrder.setDateUpdated(new Date());
                existingOrder.setUpdatedBy(loggedInUser.getUsername());
                existingOrder.setStatus(constantUtil.PENDING_NEGOTIATION);
                orderRepository.save(existingOrder);

                saveComments(request.getDescription(),existingOrder,loggedInUser);
                log.info("Initiating sending of notification......");
                String message = "Teller has sent a rate request for ORDER "+existingOrder.getOrderId();
                List<Status> statusList = Collections.singletonList(constantUtil.ACTIVE);
                User dealer = userRepo.findDistinctByUsernameEqualsIgnoreCaseAndStatusIn(existingOrder.getDealerId(),statusList);
                if(dealer!=null){
                    notificationService.sendUserNotification(message,existingOrder,dealer,NotificationType.NEGOTIATE_RATE);
                    log.info("Notification successfully sent to dealer {} .......",dealer.getUsername());
                }

                log.info("Order request successfully sent back to dealer {}",existingOrder.getId());
                response.setResponseMessage("Order request successfully sent back to dealer.");
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

    public ApiResponse fetchAccounts(String accountNumber){
        ApiResponse response = new ApiResponse();
        try {
            log.info("Fetching Details for accNo:  {}",accountNumber);
            AccountDetailsResponse accountDetailsResponse = accountDetailsService.getAccountDetails(accountNumber);
            if(accountDetailsResponse.getResponseCode().equals(ApiResponseCode.SUCCESS.getCode())){
                log.info("Details fetched successfully for accNo:  {}",accountNumber);
                response.setResponseMessage(accountDetailsResponse.getResponseMessage());
                response.setResponseCode(ApiResponseCode.SUCCESS);
                response.setEntity(accountDetailsResponse.getAccountDetails());
            }
            else{
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage(accountDetailsResponse.getResponseMessage());
                log.error("Account details fetch error {}",accountDetailsResponse.getResponseMessage());
            }
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING APPROVAL OF ACCOUNTS: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred during approval of accounts");
        }
        return response;
    }

    public ApiResponse getAllExchangeRates() {
        ApiResponse response = new ApiResponse();
        try {
            SOAResponse soaResponse = getExchangeRateClient.getAllExchangeRates();

            if (ApiResponseCode.SUCCESS.getCode().equals(soaResponse.getResponseCode())) {
                log.info("All exchange rates fetched successfully. Retrieved {} currency pairs",
                        ((List<?>) soaResponse.getData()).size());
                response.setResponseCode(ApiResponseCode.SUCCESS);
                response.setResponseMessage(soaResponse.getMessage());
                response.setEntity(soaResponse.getData());

            } else {
                log.error("Failed to get all exchange rates. Error: {}", soaResponse.getMessage());

                String message = soaResponse.getMessage() != null ?
                        soaResponse.getMessage() :
                        "Error getting all exchange rates, please try again";

                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage(message);
            }

        } catch (Exception e) {
            log.error("Unexpected error getting all exchange rates: {}", e.getMessage(), e);
            String message = "Unexpected error occurred while fetching all exchange rates. Please try again.";
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage(message);
        }

        return response;
    }


    public ApiResponse retryPostDealCode(DealCodeRetryRequest request){
        ApiResponse response = new ApiResponse();
        try {
            Optional<Order> order = orderRepository.findById(request.getId());
            if(order.isEmpty()){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Order selected does not exist");
                return response;
            }

            Order existingOrder = order.get();
            try {
                existingOrder.setStatus(constantUtil.PROCESSING);
                orderRepository.save(existingOrder);
                log.info("Initiating retry Process for Order {}..........", existingOrder.getOrderId());
                PostDealCodeResponse postDealCodeResponse = postDealCodeService.postDealCode(existingOrder);

                if (postDealCodeResponse.getResponseCode().equalsIgnoreCase(ApiResponseCode.SUCCESS.getCode())) {
                    updateOrderAndCreateApprovedDeals(existingOrder);
                    log.info("Deal Order Request {}  successfully  approved", existingOrder.getId());
                    response.setResponseMessage("Deal Request successfully Approved.");


                    log.info("Initiating sending of notification......");
                    String message = "Teller has approved the rate "+String.format("%.2f", existingOrder.getNegotiatedRate())+" for ORDER "+existingOrder.getOrderId();
                    List<Status> statusList = Collections.singletonList(constantUtil.ACTIVE);
                    User dealer = userRepo.findDistinctByUsernameEqualsIgnoreCaseAndStatusIn(existingOrder.getDealerId(),statusList);
                    if(dealer!=null){
                        notificationService.sendUserNotification(message,existingOrder,dealer,NotificationType.RFQ_APPROVED);
                        log.info("Notification successfully sent to dealer {} .......",dealer.getUsername());
                    }

                    Map<String, String> dealInfo = new HashMap<>();
                    dealInfo.put("dealCode", existingOrder.getDealerCode());
                    response.setEntity(dealInfo);
                    response.setResponseCode(ApiResponseCode.SUCCESS);
                } else {
                    existingOrder.setStatus(constantUtil.FAILED);
                    orderRepository.save(existingOrder);
                    log.info("Retry Process Failed for Order {}..........", existingOrder.getOrderId());
                    response.setResponseCode(ApiResponseCode.FAIL);
                    response.setResponseMessage("Failed to post deal code to Finacle");
                }
            }
            catch (Exception e){
                log.error("ERROR OCCURRED DURING RETRY OF POSTING DEAL CODE TO FINACLE: {}" ,e.getMessage());
                e.printStackTrace();
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Sorry,Error occurred during reposting of deal code to finacle");
                existingOrder.setStatus(constantUtil.FAILED);
                orderRepository.save(existingOrder);
                return response;
            }
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING RETRY OF POSTING DEAL CODE TO FINACLE: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred during reposting of deal code to finacle");
        }
        return response;

    }


    public void updateOrderAndCreateApprovedDeals(Order existingOrder){
        existingOrder.setStatus(constantUtil.ACTIVE);
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


        if (existingOrder.getBuySell().equalsIgnoreCase("BUY")) {
            approvedDeal.setBoughtAmount(existingOrder.getCounterNominalAmount());
        } else if (existingOrder.getBuySell().equalsIgnoreCase("SELL")) {
            approvedDeal.setSoldAmount(existingOrder.getCounterNominalAmount());
        }

        approvedDealsRepo.save(approvedDeal);
        log.info("Approved Deals staged successfully , {}", approvedDeal);
    }


    public ApiResponse validateAmountInput(AmountValidationDTO request,HttpServletResponse httpServletResponse){
        ApiResponse response = new ApiResponse();
        try {
            ObjectMapper mapper = new ObjectMapper();

            List<AmountConfiguration> configurations = amountConfigurationRepo.findAll();
            BigDecimal limitAmount = BigDecimal.ZERO;
            if(!configurations.isEmpty()){
                limitAmount = configurations.getFirst().getCounterNominalAmount();
            }

            BigDecimal amount = limitAmount;
            String standardCurrency = environment.getProperty("rfq.standardCurrency","USD");

            if(!request.getCurrency().equalsIgnoreCase(standardCurrency)) {
                CurrencyAction action = determineCurrencyActionExplicitViaSoa("USD",request.getCurrency());

                ExchangeRequest exchangeRequest = ExchangeRequest.builder()
                        .transactionAmount(String.valueOf(limitAmount))
                        .fromCurrency("USD")
                        .toCurrency(request.getCurrency())
                        .build();


                ApiResponse currencyResponse = getSinglePairExchangeRate(exchangeRequest, httpServletResponse);
                if (!currencyResponse.getResponseCode().equals(ApiResponseCode.SUCCESS)) {
                    return currencyResponse;
                }
                Map<String, Object> responseData = mapper.convertValue(currencyResponse.getEntity(),
                        new TypeReference<Map<String, Object>>() {
                        });


                if (action.equals(CurrencyAction.Buy)) {
                    amount = new BigDecimal(responseData.getOrDefault("buyingConvertedAmount", "0").toString());
                } else {
                    amount = new BigDecimal(responseData.getOrDefault("sellingConvertedAmount", "0").toString());
                }
            }

            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Amount is valid");
            response.setEntity(amount);
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING VALIDATION OF AMOUNT: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred when fetching amount threshold");
        }
        return response;
    }


    public ApiResponse validateAvailabilitySchedule(HttpServletResponse httpServletResponse){
        ApiResponse response = new ApiResponse();
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE");
            String day = now.format(dayFormatter);
            Optional<AvailabilityConfiguration> availabilityConfiguration = availabilityConfigRepo.findByDayAndStatus(day,constantUtil.ACTIVE);
            if(availabilityConfiguration.isEmpty()){
                response.setResponseCode(ApiResponseCode.SUCCESS);
                response.setResponseMessage("Portal is closed for utilization");
                response.setEntity(false);
                return response;
            }
            AvailabilityConfiguration schedule = availabilityConfiguration.get();

            LocalTime timeNow = LocalTime.now();

            if(!timeNow.isBefore(schedule.getStartTime()) && !timeNow.isAfter(schedule.getEndTime())){
                response.setResponseCode(ApiResponseCode.SUCCESS);
                response.setResponseMessage("Portal is available for utilization");
                response.setEntity(true);
                return response;
            }
            else{
                response.setResponseCode(ApiResponseCode.SUCCESS);
                response.setResponseMessage("Portal is closed for utilization");
                response.setEntity(false);
                return response;
            }
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING FETCHING AVAILABILITY SCHEDULE: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred fetching availability schedule");
        }
        return response;
    }
    public ApiResponse updateAvailabilitySchedule(UpdateScheduleDTO updateScheduleDTO ,HttpServletResponse httpServletResponse){
        ApiResponse response = new ApiResponse();
        try{
            User user = getauthenticatedAPIUser();
            List<AvailabilityItem> items = updateScheduleDTO.getItems();
            if(items.isEmpty()){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Invalid availability scedule input");
                return response;
            }

            List<AvailabilityConfiguration> configurationList = new ArrayList<>();
            items.forEach(item -> {
                try {
                    Optional<AvailabilityConfiguration> availabilityConfiguration = availabilityConfigRepo.findByDay(item.getDay());
                    int status = item.isEnabled()? 1: 0;
                    if (availabilityConfiguration.isEmpty()) {
                        AvailabilityConfiguration configuration = AvailabilityConfiguration.builder()
                                .day(item.getDay())
                                .startTime(item.getStartTime() != null  && !item.getStartTime().isEmpty() ?LocalTime.parse(item.getStartTime()):null)
                                .endTime(item.getEndTime() != null  && !item.getEndTime().isEmpty() ?LocalTime.parse(item.getEndTime()):null)
                                .status(commonTasks.getStatus(status))
                                .createdBy(user.getUsername())
                                .updatedBy(user.getUsername())
                                .build();
                        configurationList.add(configuration);
                    } else {
                        AvailabilityConfiguration configuration = availabilityConfiguration.get();
                        configuration.setStartTime(item.getStartTime() != null  && !item.getStartTime().isEmpty() ?LocalTime.parse(item.getStartTime()):null);
                        configuration.setEndTime(item.getEndTime() != null  && !item.getEndTime().isEmpty() ?LocalTime.parse(item.getEndTime()):null);
                        configuration.setStatus(commonTasks.getStatus(status));
                        configuration.setUpdatedBy(user.getUsername());
                        availabilityConfigRepo.save(configuration);
                        configurationList.add(configuration);
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                    log.error("error in this iteration of item {}",item);
                }
            });

            availabilityConfigRepo.saveAll(configurationList);
            log.info("Configurations Successfully updated");
            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Configurations sucessfully updated");
            return response;

        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING UPDATE OF AVAILABILITY SCHEDULE: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred during update of availability schedule");
        }
        return response;


    }

    public ReportResponse fetchAvailabilitySchedule(HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        try{
            List<AvailabilityConfiguration> configurations = availabilityConfigRepo.findAll();
            List<AvailabilityItem> items = new ArrayList<>();

            if(!configurations.isEmpty()) {
                configurations.forEach(configuration -> {
                    AvailabilityItem item = AvailabilityItem.builder()
                            .day(configuration.getDay())
                            .key(configuration.getDay().substring(0, 3).toLowerCase())
                            .enabled(configuration.getStatus().equals(constantUtil.ACTIVE))
                            .startTime(String.valueOf(configuration.getStartTime()))
                            .endTime(String.valueOf(configuration.getEndTime()))
                            .build();
                    items.add(item);
                });
            }
            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Configurations successfully fetched");
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(items), ArrayList.class));
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING FETCH OF SCHEDULE: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred during fetching of Configurations successfully");
        }
        return response;
        }

    public ReportResponse fetchBranches(HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        try{
            List<Branch> branches = branchRepo.findAll();

            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("branches successfully fetched");
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(branches), ArrayList.class));
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING FETCH OF BRANCHES: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred during fetching of Branches successfully");
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        }
        return response;
    }


    public ApiResponse fetchAmountConfiguration(HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        try{
            List<AmountConfiguration> configurations = amountConfigurationRepo.findAll();
            BigDecimal amount = BigDecimal.ZERO;
            if(!configurations.isEmpty()){
                amount = configurations.getFirst().getCounterNominalAmount();
            }

            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Configurations successfully fetched");
            response.setData(amount);
            log.info("Configurations successfully fetched");
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING FETCH OF AMOUNT CONFIGURATION: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred during fetching of amount config");
        }
        return response;
    }


    public ApiResponse updateAmountConfiguration(UpdateAmountLimitConfigDTO request,HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        try{
            List<AmountConfiguration> configurations = amountConfigurationRepo.findAll();
            BigDecimal amount = BigDecimal.ZERO;
            if(configurations.isEmpty()){
                AmountConfiguration  amountConfiguration = AmountConfiguration.builder()
                        .counterNominalAmount(request.getAmount())
                        .build();
                amountConfigurationRepo.save(amountConfiguration);
            }
            else{
                configurations.forEach(configuration -> {
                   configuration.setCounterNominalAmount(request.getAmount());
                   amountConfigurationRepo.save(configuration);
                });

            }

            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Configurations successfully updated");
            response.setData(amount);
            log.info("Configurations successfully updated");
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING UPDATE OF AMOUNT THRESHOLD: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred during update of Amount threshold configuration");
        }
        return response;
    }



}
