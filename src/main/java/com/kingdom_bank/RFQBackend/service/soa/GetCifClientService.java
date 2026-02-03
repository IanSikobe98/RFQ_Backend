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


                String statusCode = StringUtils.substringBetween(response.getBody(), "<ns3:Status>", "</ns3:Status>");
                cifResponse.setResponseMessage(statusCode);
                if (statusCode != null && statusCode.equalsIgnoreCase("SUCCESS")) {
                    cifResponse.setResponseCode(ApiResponseCode.SUCCESS.getCode());
                    String cif = StringUtils.substringBetween(response.getBody(), "<ns3:CifId>", "</ns3:CifId>");

                    AccountDetailsDTO accountDetailsDTO = AccountDetailsDTO
                            .builder().customerCode(cif).build();
                    cifResponse.setAccountDetails(accountDetailsDTO);
                }else {
                    String message = StringUtils.substringBetween(response.getBody(), "<ns3:ErrorDesc>", "</ns3:ErrorDesc>");
                    cifResponse.setResponseMessage(message);
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
        String formattedDate = DateTimeFormatter.ISO_INSTANT
                .format(Instant.now().truncatedTo(ChronoUnit.MILLIS));

        return String.format(
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "   <soapenv:Header>\n" +
                        "      <RequestHeader xmlns=\"https://kingdombankltd.co.ke/banking/core\">\n" +
                        "         <RequestId>%s</RequestId>\n" +
                        "         <ChannelId>COR</ChannelId>\n" +
                        "         <Timestamp>%s</Timestamp>\n" +
                        "      </RequestHeader>\n" +
                        "   </soapenv:Header>\n" +
                        "   <soapenv:Body>\n" +
                        "      <CustomerIDInquiry xmlns=\"https://kingdombankltd.co.ke/banking/core\">\n" +
                        "         <docType>%s</docType>\n" +
                        "         <docId>%s</docId>\n" +
                        "      </CustomerIDInquiry>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>",
                uid,formattedDate, documentType, documentNumber
        );
    }}
