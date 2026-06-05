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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AccountDetailsService {
    private final Logger log = LoggerFactory.getLogger(AccountDetailsService.class);


    @Value("${soa.accountdetails.endpoint}")
    private String accountDetailsEndpoint;

    @Value("${soa.channel.id}")
    private String channelId;

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
                String statusCode = getTagValue(response.getBody(), "Status");
//                String messageCode = StringUtils.substringBetween(response.getBody(), "<head:MessageCode>", "</head:MessageCode>");
//                String message = StringUtils.substringBetween(response.getBody(), "<head:MessageDescription>", "</head:MessageDescription>");
                balanceInquiryResponse.setResponseMessage(statusCode);
                if (statusCode != null && statusCode.equalsIgnoreCase("SUCCESS")) {
                    AccountDetailsDTO accountDetailsDTO = AccountDetailsDTO.builder()
                            .customerCode(getTagValue(response.getBody(),"CustomerId"))
                            .mobileNumber(getTagValue( response.getBody(),"MobileNumber"))
                            .accountStatus(getTagValue(response.getBody(),"AccountStatus"))
                            .accountOpenDate(getTagValue(response.getBody(),"AccountOpenDate"))
                            .accountName(getTagValue(response.getBody(),"AccountName"))
                            .currencyCode(getTagValue(response.getBody(),"Currency"))
                            .productId(getTagValue(response.getBody(),"SchemeType"))
                            .productContextCode(getTagValue(response.getBody(),"SchemeCode"))
                            .productName(getTagValue(response.getBody(),"SchemeCodeDesc"))
                            .branchCode(getTagValue(response.getBody(),"BranchId"))
                            .balance(getTagValue(response.getBody(),"AvailableBalance"))
                            .build();
                    balanceInquiryResponse.setAccountDetails(accountDetailsDTO);
                    balanceInquiryResponse.setResponseCode(ApiResponseCode.SUCCESS.getCode());
                }
                else {
                    String message = StringUtils.substringBetween(response.getBody(), "<ns2:ErrorDesc>", "</ns2:ErrorDesc>");
                    balanceInquiryResponse.setResponseMessage(message);
                }
            }
            else{
                balanceInquiryResponse.setResponseMessage("Error Occurred while fetching account details");
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
//        String formattedDate = fd.format(now);
        String formattedDate = DateTimeFormatter.ISO_INSTANT
                .format(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        String request = String.format("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soapenv:Header>\n" +
                "        <RequestHeader xmlns=\"https://kingdombankltd.co.ke/banking/core\">\n" +
                "            <RequestId>%s</RequestId>\n" +
                "            <ChannelId>%s</ChannelId>\n" +
                "            <Timestamp>%s</Timestamp>\n" +
                "        </RequestHeader>\n" +
                "    </soapenv:Header>\n" +
                "    <soapenv:Body>\n" +
                "        <GetAccountDetails xmlns=\"https://kingdombankltd.co.ke/banking/core\">\n" +
                "            <AccountID>%s</AccountID>\n" +
                "        </GetAccountDetails>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>", uid,channelId,formattedDate, accountNumber);


        return request;
    }

//    private String extractResponseDetail(String key,String response){
//        return  StringUtils.substringBetween(response, String.format("<ns2:%s>",key), String.format("</ns2:%s>",key));
//    }

    private String getTagValue(String xml, String tagName) {
        Pattern pattern = Pattern.compile(
                "<\\w+:" + Pattern.quote(tagName) + ">(.*?)</\\w+:" + Pattern.quote(tagName) + ">",
                Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(xml);
        return matcher.find() ? matcher.group(1) : null;
    }

}
