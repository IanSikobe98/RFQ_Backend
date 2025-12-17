package com.kingdom_bank.RFQBackend.service.soa;



import com.kingdom_bank.RFQBackend.dto.AccountDetailsDTO;
import com.kingdom_bank.RFQBackend.dto.AccountDetailsResponse;
import com.kingdom_bank.RFQBackend.enums.ApiResponseCode;
import com.kingdom_bank.RFQBackend.util.SoaRequestTemplateUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
public class AccountDetailsService {
    private final Logger log = LoggerFactory.getLogger(AccountDetailsService.class);


    @Value("${soa.accountdetails.endpoint}")
    private String accountDetailsEndpoint;

    private final SoaRequestTemplateUtil soaRequestTemplateUtil;

    public AccountDetailsService(SoaRequestTemplateUtil soaRequestTemplateUtil) {
        this.soaRequestTemplateUtil = soaRequestTemplateUtil;
    }



    public AccountDetailsResponse getAccountDetails(String accountNumber){
        AccountDetailsResponse balanceInquiryResponse = AccountDetailsResponse.builder().responseCode(ApiResponseCode.FAIL.getCode()).build();
        try {
            ResponseEntity<String> response = soaRequestTemplateUtil.sendSoaRequest("GetAccountDetails", "GetAccountDetails", accountDetailsEndpoint, accountInquiryRequest(accountNumber),"1"
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                String statusCode = StringUtils.substringBetween(response.getBody(), "<head:StatusCode>", "</head:StatusCode>");
                String messageCode = StringUtils.substringBetween(response.getBody(), "<head:MessageCode>", "</head:MessageCode>");
                String message = StringUtils.substringBetween(response.getBody(), "<head:MessageDescription>", "</head:MessageDescription>");
                balanceInquiryResponse.setResponseMessage(message);
                if (statusCode != null && statusCode.equalsIgnoreCase("S_001")
                        && messageCode != null && messageCode.equalsIgnoreCase("0")) {
                    AccountDetailsDTO accountDetailsDTO = AccountDetailsDTO.builder()
                            .customerCode(extractResponseDetail("CustomerCode", response.getBody()))
                            .accountName(extractResponseDetail("AccountName", response.getBody()))
                            .currencyCode(extractResponseDetail("CurrencyCode", response.getBody()))
                            .productId(extractResponseDetail("ProductID", response.getBody()))
                            .productContextCode(extractResponseDetail("ProductContextCode", response.getBody()))
                            .productName(extractResponseDetail("ProductName", response.getBody()))
                            .branchCode(extractResponseDetail("BranchCode", response.getBody()))
                            .build();
                    balanceInquiryResponse.setAccountDetails(accountDetailsDTO);
                    balanceInquiryResponse.setResponseCode(ApiResponseCode.SUCCESS.getCode());
                }
            }
        } catch (Exception e) {
            log.error("getBalanceInquiry:idNumber {}:: Error", accountNumber, e);
            balanceInquiryResponse.setResponseMessage("Error Occurred while fetching account details");
        }
        log.info(String.valueOf(balanceInquiryResponse));
        return balanceInquiryResponse;
    }



    private String accountInquiryRequest(String accountNumber) {


        String uid = UUID.randomUUID().toString();

        SimpleDateFormat fd = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date now = new Date();
        String formattedDate = fd.format(now);
        String request = String.format("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mes=\"urn://co-opbank.co.ke/CommonServices/Data/Message/MessageHeader\" xmlns:com=\"urn://co-opbank.co.ke/CommonServices/Data/Common\" xmlns:bsac=\"urn://co-opbank.co.ke/BS/Account/BSAccountDetails.3.0\">\n" +
                "   <soapenv:Header>\n" +
                "      <mes:RequestHeader>\n" +
                "           <com:CreationTimestamp>%s</com:CreationTimestamp>\n" +
                "         <!--Optional:-->\n" +
                "         <com:CorrelationID>%s</com:CorrelationID>\n" +
                "         <!--Optional:-->\n" +
                "         <mes:FaultTO/>\n" +
                "         <mes:MessageID>%s</mes:MessageID>\n" +
                "         <!--Optional:-->\n" +
                "         <mes:ReplyTO/>\n" +
                "         <!--Optional:-->\n" +
                "         <mes:Credentials>\n" +
                "            <!--Optional:-->\n" +
                "            <mes:SystemCode>0000</mes:SystemCode>\n" +
                "            <!--Optional:-->\n" +
                "            <mes:Username/>\n" +
                "            <!--Optional:-->\n" +
                "            <mes:Password/>\n" +
                "            <!--Optional:-->\n" +
                "            <mes:Realm/>\n" +
                "            <!--Optional:-->\n" +
                "            <mes:BankID>01</mes:BankID>\n" +
                "         </mes:Credentials>\n" +
                "      </mes:RequestHeader>\n" +
                "   </soapenv:Header>\n" +
                "   <soapenv:Body>\n" +
                "      <bsac:AccountDetailsRequest>\n" +
                "         <!--Optional:-->\n" +
                "         <bsac:AccountNumber>%s</bsac:AccountNumber>\n" +
                "      </bsac:AccountDetailsRequest>\n" +
                "   </soapenv:Body>\n" +

                "</soapenv:Envelope>", formattedDate, uid, uid, accountNumber);


        return request;
    }

    private String extractResponseDetail(String key,String response){
        return  StringUtils.substringBetween(response, String.format("<tns25:%s>",key), String.format("</tns25:%s>",key));
    }

}
