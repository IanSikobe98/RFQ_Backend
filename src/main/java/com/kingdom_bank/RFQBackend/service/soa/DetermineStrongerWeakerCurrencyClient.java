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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
                String statusCode = StringUtils.substringBetween(response.getBody(), "<ns2:Status>", "</ns2:Status>");


                if (statusCode != null && statusCode.equalsIgnoreCase("SUCCESS"))
                {
                    // Extract exchange rate data
                    String responseFromCurrency = StringUtils.substringBetween(response.getBody(), "<ns2:FromCurrency>", "</ns2:FromCurrency>");
                    String responseToCurrency = StringUtils.substringBetween(response.getBody(), "<ns2:ToCurrency>", "</ns2:ToCurrency>");
                    // Extract exchange rate data
                    String exchangeRate = StringUtils.substringBetween(response.getBody(), "<ns2:ExchangeRate>", "</ns2:ExchangeRate>");
                    String convertedAmount = StringUtils.substringBetween(response.getBody(), "<ns2:ConvertedAmount>", "</ns2:ConvertedAmount>");
                    String multiplyDivide = StringUtils.substringBetween(response.getBody(), "<ns2:MultiplyDivide>", "</ns2:MultiplyDivide>");


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
                    String message = StringUtils.substringBetween(response.getBody(), "<ns2:ErrorDesc>", "</ns2:ErrorDesc>");
                    soaResponse.setMessage(message);
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
        String formattedDate = DateTimeFormatter.ISO_INSTANT
                .format(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        return String.format(
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "    <soapenv:Header>\n" +
                        "        <RequestHeader xmlns=\"https://kingdombankltd.co.ke/banking/core\">\n" +
                        "            <RequestId>%s</RequestId>\n" +
                        "            <ChannelId>COR</ChannelId>\n" +
                        "            <Timestamp>%s</Timestamp>\n" +
                        "        </RequestHeader>\n" +
                        "    </soapenv:Header>\n" +
                        "    <soapenv:Body>\n" +
                        "        <GetExchangeRates xmlns=\"https://kingdombankltd.co.ke/banking/core\">\n" +
                        "            <TransactionAmount>%s</TransactionAmount>\n" +
                        "            <RateCode>MID</RateCode>\n" +
                        "            <ToCurrency>%s</ToCurrency>\n" +
                        "            <FromCurrency>%s</FromCurrency>\n" +
                        "        </GetExchangeRates>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>",
                uid, formattedDate, transactionAmount, toCurrency , fromCurrency
        );
    }
}