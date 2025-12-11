package com.kingdom_bank.RFQBackend.service.soa;


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
import java.util.*;


@Service
public class GetCustomerAccounts {
    private final Logger log = LoggerFactory.getLogger(GetCustomerAccounts.class);

    @Value("${soa.getCustomerAccounts.endpoint}")
    private String getCustomerAccountsEndpoint;

    private final SoaRequestTemplateUtil soaRequestTemplateUtil;

    public GetCustomerAccounts(SoaRequestTemplateUtil soaRequestTemplateUtil) {
        this.soaRequestTemplateUtil = soaRequestTemplateUtil;
    }

    public CustomerAccountsResponse getCustomerAccounts(String cif, String channel) {
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
                String statusCode = StringUtils.substringBetween(response.getBody(), "<head:StatusCode>", "</head:StatusCode>");
                String messageCode = StringUtils.substringBetween(response.getBody(), "<head:MessageCode>", "</head:MessageCode>");
                String message = StringUtils.substringBetween(response.getBody(), "<head:MessageDescription>", "</head:MessageDescription>");
                CustomerAccountSummary summary = parseAccountsFromResponse(response.getBody(), cif);
                customerAccountsResponse.setCustomerAccountSummary(summary);

                customerAccountsResponse.setResponseMessage(message);

                if (statusCode != null && statusCode.equalsIgnoreCase("S_001") &&
                        messageCode != null && messageCode.equalsIgnoreCase("0")) {
                    customerAccountsResponse.setResponseCode(ApiResponseCode.SUCCESS.getCode());
                } else {
                    customerAccountsResponse.setCustomerAccountSummary(null);
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date now = new Date();
        String formattedDate = dateFormat.format(now);

        return String.format(
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "xmlns:mes=\"urn://co-opbank.co.ke/CommonServices/Data/Message/MessageHeader\" " +
                        "xmlns:com=\"urn://co-opbank.co.ke/CommonServices/Data/Common\" " +
                        "xmlns:cus=\"urn://co-opbank.co.ke/BS/Customer/CustomerAccount.Get.3.0\">\n" +
                        "   <soapenv:Header>\n" +
                        "      <mes:RequestHeader>\n" +
                        "         <com:CreationTimestamp>%s</com:CreationTimestamp>\n" +
                        "         <com:CorrelationID>%s</com:CorrelationID>\n" +
                        "         <mes:FaultTO></mes:FaultTO>\n" +
                        "         <mes:MessageID>%s</mes:MessageID>\n" +
                        "         <mes:ReplyTO></mes:ReplyTO>\n" +
                        "         <mes:Credentials>\n" +
                        "            <mes:SystemCode>000</mes:SystemCode>\n" +
                        "            <mes:Username></mes:Username>\n" +
                        "            <mes:Password></mes:Password>\n" +
                        "            <mes:Realm></mes:Realm>\n" +
                        "            <mes:BankID>01</mes:BankID>\n" +
                        "         </mes:Credentials>\n" +
                        "      </mes:RequestHeader>\n" +
                        "   </soapenv:Header>\n" +
                        "   <soapenv:Body>\n" +
                        "      <cus:CustomerAccDetailsRq>\n" +
                        "         <cus:CustomerId>%s</cus:CustomerId>\n" +
                        "         <cus:SchmCode></cus:SchmCode>\n" +
                        "      </cus:CustomerAccDetailsRq>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>",
                formattedDate, uid, uid, cif
        );
    }

    private CustomerAccountSummary parseAccountsFromResponse(String xmlResponse, String cif) {
        List<CustomerAccount> accounts = new ArrayList<>();

        try {
            // Split response by account records
            String[] accountRecords = xmlResponse.split("<tns28:CustAccLLRec>");

            for (int i = 1; i < accountRecords.length; i++) {
                String record = accountRecords[i];

                CustomerAccount account = new CustomerAccount();

                String phoneNumber = StringUtils.substringBetween(record, "<tns28:PhoneNumber>", "</tns28:PhoneNumber>");
                String accountNumber = StringUtils.substringBetween(record, "<tns28:AccountNumber>", "</tns28:AccountNumber>");
                String accountName = StringUtils.substringBetween(record, "<tns28:AccountName>", "</tns28:AccountName>");
                String currency = StringUtils.substringBetween(record, "<tns28:Currency>", "</tns28:Currency>");
                String accountType = StringUtils.substringBetween(record, "<tns28:AccountType>", "</tns28:AccountType>");
                String accountCode = StringUtils.substringBetween(record, "<tns28:AccountCode>", "</tns28:AccountCode>");
                String accountDescription = StringUtils.substringBetween(record, "<tns28:AccountDescription>", "</tns28:AccountDescription>");
                String accountStatus = StringUtils.substringBetween(record, "<tns28:AccountStatus>", "</tns28:AccountStatus>");
                String balance = StringUtils.substringBetween(record, "<tns28:Balance>", "</tns28:Balance>");
                String freezeCode = StringUtils.substringBetween(record, "<tns28:FreezeCode>", "</tns28:FreezeCode>");
                if (freezeCode == null && record.contains("<tns28:FreezeCode/>")) {
                    freezeCode = "";
                }
                String accountClosureFlag = StringUtils.substringBetween(record, "<tns28:AccountClosureFlag>", "</tns28:AccountClosureFlag>");
                String accountOpenDate = StringUtils.substringBetween(record, "<tns28:AccountOpenDate>", "</tns28:AccountOpenDate>");
                String customerCif = cif;
                if (customerCif == null || customerCif.isEmpty()) {
                    customerCif = cif;
                }

                System.out.println("CustomerCif===>" + customerCif);


                // Set all fields
                account.setPhoneNumber(phoneNumber);
                account.setAccountNumber(accountNumber);
                account.setAccountName(accountName);
                account.setCurrency(currency);
                account.setAccountType(accountType);
                account.setAccountDescription(accountDescription);
                account.setAccountStatus(accountStatus);
                account.setBalance(balance);
                account.setAccountClosureFlag(accountClosureFlag);
                account.setAccountOpenDate(accountOpenDate);
                account.setCustomerCif(customerCif);
                account.setFreezeCode(freezeCode);
                account.setAccountCode(accountCode);

                if (account.getAccountCode() != null && !account.getAccountCode().isEmpty() && account.getAccountCode().equalsIgnoreCase("STCUR")) {
                    account.setIsStaffAccount(true);
                }

                if ("A".equalsIgnoreCase(accountStatus) &&
                        "N".equalsIgnoreCase(accountClosureFlag) &&
                        (freezeCode == null || freezeCode.isEmpty()) &&
                        !Arrays.asList("LAA", "TDA", "TUA", "ODA").contains(accountType)) {
                    accounts.add(account);
                }
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

        // Get full name from any account (first one)
        String fullName = accounts.get(0).getAccountName();
        String customerCif = accounts.get(0).getCustomerCif();

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
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            Date d1 = sdf.parse(date1);
            Date d2 = sdf.parse(date2);
            return d1.compareTo(d2);
        } catch (Exception e) {
            log.warn("Error comparing dates: {} and {}", date1, date2);
            return 0;
        }
    }

    private String extractYear(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            Date date = sdf.parse(dateString);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return String.valueOf(cal.get(Calendar.YEAR));
        } catch (Exception e) {
            log.warn("Error extracting year from date: {}", dateString);
            return "";
        }
    }
}

