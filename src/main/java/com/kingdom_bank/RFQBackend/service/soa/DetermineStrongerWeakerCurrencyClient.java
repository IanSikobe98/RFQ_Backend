package com.kingdom_bank.RFQBackend.service.soa;

import com.kingdom_bank.RFQBackend.dto.SOAResponse;
import com.kingdom_bank.RFQBackend.dto.SoaGetStrongerWeakerDto;
import com.kingdom_bank.RFQBackend.enums.ApiResponseCode;
import com.kingdom_bank.RFQBackend.util.SoaRequestTemplateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

@Service
@Slf4j
public class DetermineStrongerWeakerCurrencyClient {

    @Value("${soa.getExchangeRate.endpoint}")
    private String getExchangeRateEndpoint;

    private final SoaRequestTemplateUtil soaRequestTemplateUtil;

    public DetermineStrongerWeakerCurrencyClient(SoaRequestTemplateUtil soaRequestTemplateUtil) {
        this.soaRequestTemplateUtil = soaRequestTemplateUtil;
    }

    public SOAResponse getExchangeRate(String fromCurrency, String toCurrency, String transactionAmount) {
        SOAResponse soaResponse = new SOAResponse();
        try {
            String request = buildGetExchangeRateRequest(fromCurrency, toCurrency, transactionAmount);
            log.info("Exchange Rate Request: {}", request);

            ResponseEntity<String> response = soaRequestTemplateUtil.sendSoaRequest(
                    "GetExchangeRate",
                    "GetExchangeRate",
                    getExchangeRateEndpoint,
                    request,"1"
            );

            log.info("Get Exchange Rate response: {}", response.getBody());


            if (response.getStatusCode().is2xxSuccessful()) {
                String statusCode = StringUtils.substringBetween(response.getBody(), "<head:StatusCode>", "</head:StatusCode>");
                String messageCode = StringUtils.substringBetween(response.getBody(), "<head:MessageCode>", "</head:MessageCode>");
                String message = StringUtils.substringBetween(response.getBody(), "<head:MessageDescription>", "</head:MessageDescription>");

                // Extract exchange rate data
                String exchangeRate = StringUtils.substringBetween(response.getBody(), "<tns25:ExchangeRate>", "</tns25:ExchangeRate>");
                String responseFromCurrency = StringUtils.substringBetween(response.getBody(), "<tns25:FromCurrency>", "</tns25:FromCurrency>");
                String responseToCurrency = StringUtils.substringBetween(response.getBody(), "<tns25:ToCurrency>", "</tns25:ToCurrency>");
                String convertedAmount = StringUtils.substringBetween(response.getBody(), "<tns25:ConvertedAmount>", "</tns25:ConvertedAmount>");
                String multiplyDivide = StringUtils.substringBetween(response.getBody(), "<tns25:MultiplyDivide>", "</tns25:MultiplyDivide>");

                soaResponse.setMessage(message);

                if (statusCode != null && statusCode.equalsIgnoreCase("S_001") &&
                        messageCode != null && (messageCode.equalsIgnoreCase("0") || messageCode.equalsIgnoreCase("Y"))) {

                    SoaGetStrongerWeakerDto dto = new SoaGetStrongerWeakerDto();
                    dto.setExchangeRate(exchangeRate);
                    dto.setFromCurrency(responseFromCurrency);
                    dto.setToCurrency(responseToCurrency);
                    dto.setConvertedAmount(convertedAmount);
                    dto.setMultiplyDivide(multiplyDivide);

                    soaResponse.setResponseCode(ApiResponseCode.SUCCESS.getCode());
                    soaResponse.setData(dto);
                } else {
                    soaResponse.setResponseCode(ApiResponseCode.FAIL.getCode());
                }
            } else {
                soaResponse.setResponseCode(ApiResponseCode.FAIL.getCode());
                soaResponse.setMessage("HTTP Error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("GetExchangeRate:: Error for fetching exchange rate from {} to {} for amount {}",
                    fromCurrency, toCurrency, transactionAmount, e);
            soaResponse.setResponseCode(ApiResponseCode.FAIL.getCode());
            soaResponse.setMessage("Error occurred while fetching exchange rate: " + e.getMessage());
        }
        return soaResponse;
    }

    private String buildGetExchangeRateRequest(String fromCurrency, String toCurrency, String transactionAmount) {
        String uid = UUID.randomUUID().toString();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date now = new Date();
        String formattedDate = dateFormat.format(now);

        return String.format(
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "xmlns:mes=\"urn://co-opbank.co.ke/CommonServices/Data/Message/MessageHeader\" " +
                        "xmlns:com=\"urn://co-opbank.co.ke/CommonServices/Data/Common\" " +
                        "xmlns:bsc=\"urn://co-opbank.co.ke/BS/Common/BSCurrencyExchangeRate.2.0\">" +
                        "<soapenv:Header>" +
                        "<mes:RequestHeader>" +
                        "<com:CreationTimestamp>%s</com:CreationTimestamp>" +
                        "<com:CorrelationID>%s</com:CorrelationID>" +
                        "<mes:FaultTO/>" +
                        "<mes:MessageID>%s</mes:MessageID>" +
                        "<mes:ReplyTO/>" +
                        "<mes:Credentials>" +
                        "<mes:SystemCode>000</mes:SystemCode>" +
                        "<mes:Username/>" +
                        "<mes:Password/>" +
                        "<mes:Realm/>" +
                        "<mes:BankID>01</mes:BankID>" +
                        "</mes:Credentials>" +
                        "</mes:RequestHeader>" +
                        "</soapenv:Header>" +
                        "<soapenv:Body>" +
                        "<bsc:ExchangeRateRequest>" +
                        "<bsc:FromCurrency>%s</bsc:FromCurrency>" +
                        "<bsc:ToCurrency>%s</bsc:ToCurrency>" +
                        "<bsc:RateCode>MID</bsc:RateCode>" +
                        "<bsc:TransactionAmount>%s</bsc:TransactionAmount>" +
                        "<bsc:OperationType>n</bsc:OperationType>" +
                        "</bsc:ExchangeRateRequest>" +
                        "</soapenv:Body>" +
                        "</soapenv:Envelope>",
                formattedDate, uid, uid, fromCurrency, toCurrency, transactionAmount
        );
    }
}