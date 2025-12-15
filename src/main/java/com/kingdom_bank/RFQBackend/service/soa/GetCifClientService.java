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
import java.util.TimeZone;
import java.util.UUID;


@Service
public class GetCifClientService {
    private final Logger log = LoggerFactory.getLogger(GetCifClientService.class);

    @Value("${soa.getCustomerID.endpoint}")
    private String getCustomerCifEndpoint;

    private final SoaRequestTemplateUtil soaRequestTemplateUtil;

    public GetCifClientService(SoaRequestTemplateUtil soaRequestTemplateUtil) {
        this.soaRequestTemplateUtil = soaRequestTemplateUtil;
    }

    public AccountDetailsResponse getCustomerCif(String documentType, String documentNumber) {
        AccountDetailsResponse cifResponse = AccountDetailsResponse.builder().responseCode(ApiResponseCode.FAIL.getCode()).build();
        try {
            String request = buildGetCifRequest(documentType, documentNumber);
            System.out.println("BELOW IS THE REQUEST: " + request);
            ResponseEntity<String> response = soaRequestTemplateUtil.sendSoaRequest("GetCustomerID", "GetCustomerID", getCustomerCifEndpoint, request,"1");

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
                String cif = StringUtils.substringBetween(response.getBody(), "<tns29:CustomerId>", "</tns29:CustomerId>");
                cifResponse.setResponseMessage(message);

                if (statusCode != null && statusCode.equalsIgnoreCase("S_001") &&
                        messageCode != null && messageCode.equalsIgnoreCase("0")) {
                    cifResponse.setResponseCode(ApiResponseCode.SUCCESS.getCode());
                    AccountDetailsDTO accountDetailsDTO = AccountDetailsDTO
                            .builder().customerCode(cif).build();
                    cifResponse.setAccountDetails(accountDetailsDTO);
                }
            } else {
                cifResponse.setResponseMessage("HTTP Error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("GetCustomerCif:: Error for Fetching CIF for Customer with id of {}", documentNumber, e);
            cifResponse.setResponseMessage("Error occurred while fetching cif: " + e.getMessage());
        }
        return cifResponse;
    }

    private String buildGetCifRequest(String documentType, String documentNumber) {
        String uid = UUID.randomUUID().toString();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date now = new Date();
        String formattedDate = dateFormat.format(now);

        return String.format(
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "xmlns:mes=\"urn://co-opbank.co.ke/CommonServices/Data/Message/MessageHeader\" " +
                        "xmlns:com=\"urn://co-opbank.co.ke/CommonServices/Data/Common\" " +
                        "xmlns:cus=\"urn://co-opbank.co.ke/BS/Customer/CustomerId.Get.3.0\">\n" +
                        "   <soapenv:Header>\n" +
                        "      <mes:RequestHeader>\n" +
                        "         <com:CreationTimestamp>%s</com:CreationTimestamp>\n" +
                        "         <com:CorrelationID>%s</com:CorrelationID>\n" +
                        "         <mes:FaultTO/>\n" +
                        "         <mes:MessageID>%s</mes:MessageID>\n" +
                        "         <mes:ReplyTO/>\n" +
                        "         <mes:Credentials>\n" +
                        "            <mes:SystemCode>000</mes:SystemCode>\n" +
                        "            <mes:Username/>\n" +
                        "            <mes:Password/>\n" +
                        "            <mes:Realm/>\n" +
                        "            <mes:BankID/>\n" +
                        "         </mes:Credentials>\n" +
                        "      </mes:RequestHeader>\n" +
                        "   </soapenv:Header>\n" +
                        "   <soapenv:Body>\n" +
                        "      <cus:CustomerIDRq>\n" +
                        "         <cus:UniqueIdentifierType>%s</cus:UniqueIdentifierType>\n" +
                        "         <cus:UniqueIdentifierValue>%s</cus:UniqueIdentifierValue>\n" +
                        "      </cus:CustomerIDRq>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>",
                formattedDate, uid, uid, documentType, documentNumber
        );
    }}
