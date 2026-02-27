package com.kingdom_bank.RFQBackend.service.soa;


import com.kingdom_bank.RFQBackend.dto.AccountDetailsResponse;
import com.kingdom_bank.RFQBackend.dto.CustomerAccount;
import com.kingdom_bank.RFQBackend.dto.CustomerAccountSummary;
import com.kingdom_bank.RFQBackend.dto.CustomerAccountsResponse;
import com.kingdom_bank.RFQBackend.enums.ApiResponseCode;
import com.kingdom_bank.RFQBackend.util.SoaRequestTemplateUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;


@Service
public class GetCustomerAccounts {
    private final Logger log = LoggerFactory.getLogger(GetCustomerAccounts.class);

    @Value("${soa.getCustomerAccounts.endpoint}")
    private String getCustomerAccountsEndpoint;

    private final SoaRequestTemplateUtil soaRequestTemplateUtil;
    private final AccountDetailsService accountDetailsService;

    public GetCustomerAccounts(SoaRequestTemplateUtil soaRequestTemplateUtil, AccountDetailsService accountDetailsService) {
        this.soaRequestTemplateUtil = soaRequestTemplateUtil;
        this.accountDetailsService = accountDetailsService;
    }

    public CustomerAccountsResponse getCustomerAccounts(String cif) {
        CustomerAccountsResponse customerAccountsResponse = CustomerAccountsResponse.builder()
                .responseCode(ApiResponseCode.FAIL.getCode()).build();
        try {
            String request = buildGetCustomerAccountsRequest(cif);
            ResponseEntity<String> response = soaRequestTemplateUtil.sendSoaRequest(
                    "CustAcctDtlsInq",
                    "CustAcctDtlsInq",
                    getCustomerAccountsEndpoint,
                    request,"1"
            );

            //Save SOA Request
//            var saveSOARequestRes = rfqAPIPayloadService.logRequest(request, channel);


            log.info("Get Customer Cif response: {}", response.getBody());

            //Save SOA Response
//            if (saveSOARequestRes != null){
//                Long id = saveSOARequestRes.getId();
//                rfqAPIPayloadService.logResponse(id, response);
//            }


            if (response.getStatusCode().is2xxSuccessful()) {

                String statusCode = StringUtils.substringBetween(response.getBody(), "<ns3:Status>", "</ns3:Status>");
                customerAccountsResponse.setResponseMessage(statusCode);
                if (statusCode != null && statusCode.equalsIgnoreCase("SUCCESS")) {
                    customerAccountsResponse.setResponseCode(ApiResponseCode.SUCCESS.getCode());
                    CustomerAccountSummary summary = parseAccountsFromResponse(response.getBody(), cif);
                    customerAccountsResponse.setCustomerAccountSummary(summary);
                } else {
                    customerAccountsResponse.setCustomerAccountSummary(null);
                    String message = StringUtils.substringBetween(response.getBody(), "<ns2:ErrorDesc>", "</ns2:ErrorDesc>");
                    customerAccountsResponse.setResponseMessage(message);
                }
            } else {
                customerAccountsResponse.setResponseMessage("HTTP Error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("GetCustomerAccounts:: Error for Fetching Account for Customer with cif of {}", cif, e);
            customerAccountsResponse.setResponseMessage("Error occurred while fetching Accounts: " + e.getMessage());
        }
        return customerAccountsResponse;
    }

    private String buildGetCustomerAccountsRequest(String cif) {
        String uid = UUID.randomUUID().toString();
        String formattedDate = DateTimeFormatter.ISO_INSTANT
                .format(Instant.now().truncatedTo(ChronoUnit.MILLIS));

        return String.format(
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "   <soapenv:Header>\n" +
                        "      <ns1:RequestHeader xmlns:ns1=\"https://kingdombankltd.co.ke/banking/core\">\n" +
                        "         <ns1:RequestId>%s</ns1:RequestId>\n" +
                        "         <ns1:ChannelId>COR</ns1:ChannelId>\n" +
                        "         <ns1:Timestamp>%s</ns1:Timestamp>\n" +
                        "      </ns1:RequestHeader>\n" +
                        "   </soapenv:Header>\n" +
                        "   <soapenv:Body>\n" +
                        "      <ns1:GetCustomerAccountsRequest xmlns:ns1=\"https://kingdombankltd.co.ke/banking/core\">\n" +
                        "         <ns1:CustId>%s</ns1:CustId>\n" +
                        "      </ns1:GetCustomerAccountsRequest>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>\n",
                 uid, formattedDate, cif
        );
    }

    private CustomerAccountSummary parseAccountsFromResponse(String xmlResponse, String cif) {
        List<CustomerAccount> accounts = new ArrayList<>();

        try {
            // Split response by account records
            String[] accountRecords = xmlResponse.split(" <ns3:CustAccLLRec>");

            for (int i = 1; i < accountRecords.length; i++) {
                String record = accountRecords[i];

                CustomerAccount account = new CustomerAccount();

                String schemeCode = StringUtils.substringBetween(record, "<ns3:SchmCode>", "</ns3:SchmCode>");
                String accountNumber = StringUtils.substringBetween(record, "<ns3:AcctId>", "</ns3:AcctId>");
                String accountName = StringUtils.substringBetween(record, "<ns3:AcctName>", "</ns3:AcctName>");
                String currency = StringUtils.substringBetween(record, "<ns3:Crncy>", "</ns3:Crncy>");
                String accountDescription = StringUtils.substringBetween(record, "<ns3:AcctName>", "</ns3:AcctName>");
                String balance = StringUtils.substringBetween(record, "<ns3:Bal>", "</ns3:Bal>");
                String freezeCode = StringUtils.substringBetween(record, "<ns3:FreezeCode>", "</ns3:FreezeCode>");
                if (freezeCode == null && record.contains("<ns3:FreezeCode/>")) {
                    freezeCode = "";
                }
                String accountClosureFlag = StringUtils.substringBetween(record, "<ns3:AcctClsFlg>", "</ns3:AcctClsFlg>");
                String customerCif = cif;
                if (customerCif == null || customerCif.isEmpty()) {
                    customerCif = cif;
                }

                System.out.println("CustomerCif===>" + customerCif);


                // Set all fields
                account.setAccountNumber(accountNumber);
                account.setAccountName(accountName);
                account.setCurrency(currency);
                account.setAccountDescription(accountDescription);
                account.setBalance(balance);
                account.setAccountClosureFlag(accountClosureFlag);
                account.setCustomerCif(customerCif);
                account.setFreezeCode(freezeCode);
                account.setSchemeCode(schemeCode);

                if (account.getAccountCode() != null && !account.getAccountCode().isEmpty() && account.getAccountCode().equalsIgnoreCase("STCUR")) {
                    account.setIsStaffAccount(true);
                }

//              //First step of filtering out accounts
                if ( "N".equalsIgnoreCase(accountClosureFlag) &&
                        (freezeCode == null || freezeCode.trim().isEmpty()) &&
                        !Arrays.asList("LNLIQ", "SPCOL", "MORTE", "ABFLN","PSADL","INVDS").contains(schemeCode)) {
                    accounts.add(account);
                }
//                accounts.add(account);
            }

            log.info("Parsed {} accounts for customer", accounts.size());

            return createCustomerAccountSummary(accounts);

        } catch (Exception e) {
            log.error("Error parsing accounts from response", e);
            return new CustomerAccountSummary("", "", "", " ", " ", new ArrayList<>());
        }

    }

    private CustomerAccountSummary createCustomerAccountSummary(List<CustomerAccount> accounts) {
        if (accounts.isEmpty()) {
            return new CustomerAccountSummary("", "", "", " ", " ", accounts);
        }

        List<CustomerAccount> filteredAccounts = new ArrayList<>();

        accounts.forEach(account -> {
            AccountDetailsResponse accountDetailsResponse = accountDetailsService.getAccountDetails(account.getAccountNumber());
            if(accountDetailsResponse != null && accountDetailsResponse.getResponseCode().equals(ApiResponseCode.SUCCESS.getCode())) {
                account.setAccountOpenDate(accountDetailsResponse.getAccountDetails().getAccountOpenDate());
                account.setAccountStatus(accountDetailsResponse.getAccountDetails().getAccountStatus());
                account.setAccountType(accountDetailsResponse.getAccountDetails().getProductId());
                account.setPhoneNumber(accountDetailsResponse.getAccountDetails().getMobileNumber());

                if ("A".equalsIgnoreCase(account.getAccountStatus()) &&
                        !Arrays.asList("LAA", "TDA", "TUA", "ODA").contains(account.getAccountType())) {
                    filteredAccounts.add(account);
                }
            }
        });

        //update accounts
        accounts = filteredAccounts;

        if (accounts.isEmpty()) {
            return new CustomerAccountSummary("", "", "", " ", " ", accounts);
        }

        // Get full name from any account (first one)
        String fullName = accounts.getFirst().getAccountName();
        String customerCif = accounts.getFirst().getCustomerCif();

        // Find earliest account opening date for joining year
        String joiningYear = accounts.stream()
                .map(CustomerAccount::getAccountOpenDate)
                .filter(Objects::nonNull)
                .min(this::compareDates)
                .map(date -> extractYear(date))
                .orElse("");

        // Find phone number from most recent account opening date
        String phoneNumber = accounts.stream()
                .filter(account -> account.getAccountOpenDate() != null)
                .max((a1, a2) -> compareDates(a1.getAccountOpenDate(), a2.getAccountOpenDate()))
                .map(CustomerAccount::getPhoneNumber)
                .orElse(accounts.get(0).getPhoneNumber()); // fallback to first account's phone

        log.info("Customer Summary - Name: {}, Phone: {}, Joining Year: {}, Accounts: {}",
                fullName, phoneNumber, joiningYear, accounts.size());

        return new CustomerAccountSummary(phoneNumber, fullName, joiningYear,customerCif, accounts);
    }

    private int compareDates(String date1, String date2) {
        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
            LocalDateTime d1 = LocalDateTime.parse(date1, formatter);
            LocalDateTime d2 = LocalDateTime.parse(date2, formatter);

            return d1.compareTo(d2);
        } catch (Exception e) {
            log.warn("Error comparing dates: {} and {}", date1, date2);
            return 0;
        }
    }

    private String extractYear(String dateString) {
        try {
            // Define the formatter matching your input string
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
            // Parse the string into a LocalDateTime
            LocalDateTime ldt = LocalDateTime.parse(dateString, formatter);

            // Extract the year
            return String.valueOf(ldt.getYear());
        } catch (Exception e) {
            log.warn("Error extracting year from date: {}", dateString);
            return "";
        }
    }
}

