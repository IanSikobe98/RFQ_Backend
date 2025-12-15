package com.kingdom_bank.RFQBackend.service.soa;


import com.kingdom_bank.RFQBackend.dto.ExchangeRequest;
import com.kingdom_bank.RFQBackend.dto.SOAResponse;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GetExchangeRateClient {
    private final Logger log = LoggerFactory.getLogger(GetExchangeRateClient.class);

    @Value("${soa.getExchangeRate.endpoint}")
    private String getExchangeRateEndpoint;

    private final SoaRequestTemplateUtil soaRequestTemplateUtil;

    public GetExchangeRateClient( SoaRequestTemplateUtil soaRequestTemplateUtil) {
        this.soaRequestTemplateUtil = soaRequestTemplateUtil;
    }

    /**
     * Get exchange rate for a single currency pair (both buy and sell rates)
     */
    public SOAResponse getExchangeRate(ExchangeRequest exchangeRequest) {
        SOAResponse soaResponse = new SOAResponse();
        try {
            // Get buy rate (TTB)
            ExchangeRateResult buyResult = getSingleExchangeRate(exchangeRequest, "TTB");

            // Get sell rate (TTS)
            ExchangeRateResult sellResult = getSingleExchangeRate(exchangeRequest, "TTS");

            if (buyResult.isSuccess() && sellResult.isSuccess()) {
                // Build the response data
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("fromCurrency", exchangeRequest.getFromCurrency());
                responseData.put("toCurrency", exchangeRequest.getToCurrency());
                responseData.put("buyingRate", buyResult.getExchangeRate());
                responseData.put("sellingRate", sellResult.getExchangeRate());
                responseData.put("buyingConvertedAmount", buyResult.getConvertedAmount());
                responseData.put("sellingConvertedAmount", sellResult.getConvertedAmount());

                soaResponse.setResponseCode(ApiResponseCode.SUCCESS.getCode());
                soaResponse.setData(responseData);
                soaResponse.setMessage("Exchange rates retrieved successfully");
            } else {
                soaResponse.setResponseCode(ApiResponseCode.FAIL.getCode());
                String errorMessage = "Failed to retrieve exchange rates";
                if (!buyResult.isSuccess()) {
                    errorMessage += " - Buy rate error: " + buyResult.getErrorMessage();
                }
                if (!sellResult.isSuccess()) {
                    errorMessage += " - Sell rate error: " + sellResult.getErrorMessage();
                }
                soaResponse.setMessage(errorMessage);
            }

        } catch (Exception e) {
            log.error("GetExchangeRate:: Error for fetching exchange rates for {} to {}",
                    exchangeRequest.getFromCurrency(), exchangeRequest.getToCurrency(), e);
            soaResponse.setResponseCode(ApiResponseCode.FAIL.getCode());
            soaResponse.setMessage("Error occurred while fetching exchange rates: " + e.getMessage());
        }
        return soaResponse;
    }

    /**
     * Get all currency pair exchange rates (both buy and sell rates)
     */
    /**
     * Get all currency pair exchange rates (both buy and sell rates)
     */
    public SOAResponse getAllExchangeRates() {
        SOAResponse soaResponse = new SOAResponse();
        try {
            // Get all buy rates (TTB)
            Map<String, ExchangeRateItem> buyRates = getAllRatesByType("TTB");

            // Get all sell rates (TTS)
            Map<String, ExchangeRateItem> sellRates = getAllRatesByType("TTS");

            if (buyRates != null && sellRates != null) {
                List<Map<String, Object>> allPairs = new ArrayList<>();

                // Process all buy rates first
                for (String buyKey : buyRates.keySet()) {
                    ExchangeRateItem buyRate = buyRates.get(buyKey);

                    // Look for matching sell rate (check both direct and reversed key)
                    String reversedKey = buyRate.getToCurrency() + "_" + buyRate.getFromCurrency();
                    ExchangeRateItem sellRate = sellRates.get(reversedKey);

                    Map<String, Object> pairData = new HashMap<>();
                    pairData.put("fromCurrency", buyRate.getFromCurrency());
                    pairData.put("toCurrency", buyRate.getToCurrency());
                    pairData.put("combination", buyRate.getFromCurrency() + "/" + buyRate.getToCurrency());
                    pairData.put("buyingRate", buyRate.getExchangeRate());
                    pairData.put("sellingRate", sellRate != null ? sellRate.getExchangeRate() : null);

                    allPairs.add(pairData);

                    // Remove the matched sell rate to avoid duplicates
                    if (sellRate != null) {
                        sellRates.remove(reversedKey);
                    }
                }

                // Process remaining sell rates that didn't have matching buy rates
                for (String sellKey : sellRates.keySet()) {
                    ExchangeRateItem sellRate = sellRates.get(sellKey);

                    Map<String, Object> pairData = new HashMap<>();
                    pairData.put("fromCurrency", sellRate.getFromCurrency());
                    pairData.put("toCurrency", sellRate.getToCurrency());
                    pairData.put("combination", sellRate.getFromCurrency() + "/" + sellRate.getToCurrency());
                    pairData.put("buyingRate", null);
                    pairData.put("sellingRate", sellRate.getExchangeRate());

                    allPairs.add(pairData);
                }

                soaResponse.setResponseCode(ApiResponseCode.SUCCESS.getCode());
                soaResponse.setData(allPairs);
                soaResponse.setMessage("All exchange rates retrieved successfully");

                log.info("Retrieved {} currency pairs with exchange rates", allPairs.size());
            } else {
                soaResponse.setResponseCode(ApiResponseCode.FAIL.getCode());
                soaResponse.setMessage("Failed to retrieve exchange rates list");
            }

        } catch (Exception e) {
            log.error("getAllExchangeRates:: Error fetching all exchange rates", e);
            soaResponse.setResponseCode(ApiResponseCode.FAIL.getCode());
            soaResponse.setMessage("Error occurred while fetching all exchange rates: " + e.getMessage());
        }
        return soaResponse;
    }
    /**
     * Get all exchange rates for a specific rate type (TTB or TTS)
     */
    private Map<String, ExchangeRateItem> getAllRatesByType(String rateCode) {
        try {
            String request = buildGetAllExchangeRatesRequest(rateCode);
            log.info("All exchange rates request for {} rate: {}", rateCode, request);

            ResponseEntity<String> response = soaRequestTemplateUtil.sendSoaRequest(
                    "GetExchangeRate",
                    "GetExchangeRate",
                    getExchangeRateEndpoint,
                    request,"1"
            );

            /*Save SOA Request
            String channel = "15";
            var saveSOARequestRes = rfqAPIPayloadService.logRequest(request, channel);



            //Save SOA Response
            if (saveSOARequestRes != null){
                Long id = saveSOARequestRes.getId();
                rfqAPIPayloadService.logResponse(id, response);
            }
            */
            log.info("Get All Exchange Rates response for {} rate: {}", rateCode, response.getBody());



            if (response.getStatusCode().is2xxSuccessful()) {
                String statusCode = StringUtils.substringBetween(response.getBody(), "<head:StatusCode>", "</head:StatusCode>");
                String messageCode = StringUtils.substringBetween(response.getBody(), "<head:MessageCode>", "</head:MessageCode>");

                if (statusCode != null && statusCode.equalsIgnoreCase("S_001") &&
                        messageCode != null && messageCode.equalsIgnoreCase("0")) {

                    return parseExchangeRateList(response.getBody());
                } else {
                    log.error("SOA service returned error for {} rates. Status: {}, Message: {}",
                            rateCode, statusCode, messageCode);
                    return null;
                }
            } else {
                log.error("HTTP Error getting {} rates: {}", rateCode, response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error fetching {} rates", rateCode, e);
            return null;
        }
    }

    /**
     * Parse the XML response to extract exchange rate list items
     */
    private Map<String, ExchangeRateItem> parseExchangeRateList(String xmlResponse) {
        Map<String, ExchangeRateItem> rateMap = new HashMap<>();

        try {
            // Pattern to match ExchangeRateListItem elements
            Pattern itemPattern = Pattern.compile(
                    "<tns25:ExchangeRateListItem>(.*?)</tns25:ExchangeRateListItem>",
                    Pattern.DOTALL
            );

            Matcher itemMatcher = itemPattern.matcher(xmlResponse);

            while (itemMatcher.find()) {
                String itemXml = itemMatcher.group(1);

                String fromCurrency = extractValue(itemXml, "FromCurrency");
                String toCurrency = extractValue(itemXml, "ToCurrency");
                String rateCode = extractValue(itemXml, "RateCode");
                String exchangeRateStr = extractValue(itemXml, "ExchangeRate");

                if (fromCurrency != null && toCurrency != null && exchangeRateStr != null) {
                    try {
                        double exchangeRate = Double.parseDouble(exchangeRateStr);

                        ExchangeRateItem item = new ExchangeRateItem();
                        item.setFromCurrency(fromCurrency);
                        item.setToCurrency(toCurrency);
                        item.setRateCode(rateCode);
                        item.setExchangeRate(exchangeRate);

                        // Use combination of from and to currency as key
                        String key = fromCurrency + "_" + toCurrency;
                        rateMap.put(key, item);

                    } catch (NumberFormatException e) {
                        log.warn("Invalid exchange rate format: {} for {} to {}",
                                exchangeRateStr, fromCurrency, toCurrency);
                    }
                }
            }

            log.info("Parsed {} exchange rate items", rateMap.size());

        } catch (Exception e) {
            log.error("Error parsing exchange rate list", e);
        }

        return rateMap;
    }

    /**
     * Extract value from XML string using tag name
     */
    private String extractValue(String xml, String tagName) {
        return StringUtils.substringBetween(xml, "<tns25:" + tagName + ">", "</tns25:" + tagName + ">");
    }

    /**
     * Get single exchange rate (existing method)
     */
    private ExchangeRateResult getSingleExchangeRate(ExchangeRequest exchangeRequest, String rateCode) {
        ExchangeRateResult result = new ExchangeRateResult();
        try {
            String request = buildGetExchangeRateRequest(exchangeRequest, rateCode);
            log.info("Exchange rate request for {} rate: {}", rateCode, request);



            ResponseEntity<String> response = soaRequestTemplateUtil.sendSoaRequest(
                    "GetExchangeRate",
                    "GetExchangeRate",
                    getExchangeRateEndpoint,
                    request,"1"
            );

            log.info("Get Exchange Rate response for {} rate: {}", rateCode, response.getBody());




            if (response.getStatusCode().is2xxSuccessful()) {
                String statusCode = StringUtils.substringBetween(response.getBody(), "<head:StatusCode>", "</head:StatusCode>");
                String messageCode = StringUtils.substringBetween(response.getBody(), "<head:MessageCode>", "</head:MessageCode>");
                String message = StringUtils.substringBetween(response.getBody(), "<head:MessageDescription>", "</head:MessageDescription>");

                // Extract exchange rate data
                String exchangeRate = StringUtils.substringBetween(response.getBody(), "<tns25:ExchangeRate>", "</tns25:ExchangeRate>");
                String convertedAmount = StringUtils.substringBetween(response.getBody(), "<tns25:ConvertedAmount>", "</tns25:ConvertedAmount>");

                if (statusCode != null && statusCode.equalsIgnoreCase("S_001") &&
                        messageCode != null && messageCode.equalsIgnoreCase("Y")) {
                    result.setSuccess(true);
                    result.setExchangeRate(exchangeRate != null ? Double.parseDouble(exchangeRate) : 0.0);
                    result.setConvertedAmount(convertedAmount != null ? Double.parseDouble(convertedAmount) : 0.0);
                } else {
                    result.setSuccess(false);
                    result.setErrorMessage(message != null ? message : "Unknown error");
                }
            } else {
                result.setSuccess(false);
                result.setErrorMessage("HTTP Error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error fetching {} rate for {} to {}", rateCode,
                    exchangeRequest.getFromCurrency(), exchangeRequest.getToCurrency(), e);
            result.setSuccess(false);
            result.setErrorMessage("Exception: " + e.getMessage());
        }
        return result;
    }

    /**
     * Build SOAP request for single currency pair
     */
    private String buildGetExchangeRateRequest(ExchangeRequest exchangeRequest, String rateCode) {
        String uid = UUID.randomUUID().toString();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date now = new Date();
        String formattedDate = dateFormat.format(now);



        return String.format(
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "xmlns:mes=\"urn://co-opbank.co.ke/CommonServices/Data/Message/MessageHeader\" " +
                        "xmlns:com=\"urn://co-opbank.co.ke/CommonServices/Data/Common\" " +
                        "xmlns:bsc=\"urn://co-opbank.co.ke/BS/Common/BSCurrencyExchangeRate.2.0\">\n" +
                        "   <soapenv:Header>\n" +
                        "      <mes:RequestHeader>\n" +
                        "         <com:CreationTimestamp>%s</com:CreationTimestamp>\n" +
                        "         <com:CorrelationID>%s</com:CorrelationID>\n" +
                        "         <mes:MessageID>%s</mes:MessageID>\n" +
                        "         <mes:Credentials>\n" +
                        "            <mes:SystemCode>000</mes:SystemCode>\n" +
                        "            <mes:BankID>01</mes:BankID>\n" +
                        "         </mes:Credentials>\n" +
                        "      </mes:RequestHeader>\n" +
                        "   </soapenv:Header>\n" +
                        "   <soapenv:Body>\n" +
                        "      <bsc:ExchangeRateRequest>\n" +
                        "         <bsc:FromCurrency>%s</bsc:FromCurrency>\n" +
                        "         <bsc:ToCurrency>%s</bsc:ToCurrency>\n" +
                        "         <bsc:RateCode>%s</bsc:RateCode>\n" +
                        "         <bsc:Account>%s</bsc:Account>\n" +
                        "         <bsc:TransactionAmount>%s</bsc:TransactionAmount>\n" +
                        "         <bsc:OperationType>n</bsc:OperationType>\n" +
                        "      </bsc:ExchangeRateRequest>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>",
                formattedDate,
                uid,
                UUID.randomUUID().toString(),
                exchangeRequest.getFromCurrency(),
                exchangeRequest.getToCurrency(),
                rateCode,
                insertAccount(exchangeRequest),
                exchangeRequest.getTransactionAmount()
        );
    }

    public String insertAccount(ExchangeRequest exchangeRequest) {
        if (exchangeRequest.getChannel().equalsIgnoreCase("15")) {
            return "";
        } else {
            return exchangeRequest.getAccount();
        }
    }

    /**
     * Build SOAP request for all exchange rates list
     */
    private String buildGetAllExchangeRatesRequest(String rateCode) {
        String uid = UUID.randomUUID().toString();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date now = new Date();
        String formattedDate = dateFormat.format(now);

        return String.format(
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "xmlns:mes=\"urn://co-opbank.co.ke/CommonServices/Data/Message/MessageHeader\" " +
                        "xmlns:com=\"urn://co-opbank.co.ke/CommonServices/Data/Common\" " +
                        "xmlns:bsc=\"urn://co-opbank.co.ke/BS/Common/BSCurrencyExchangeRate.2.0\">\n" +
                        "   <soapenv:Header>\n" +
                        "      <mes:RequestHeader>\n" +
                        "         <com:CreationTimestamp>%s</com:CreationTimestamp>\n" +
                        "         <com:CorrelationID>%s</com:CorrelationID>\n" +
                        "         <mes:MessageID>%s</mes:MessageID>\n" +
                        "         <mes:Credentials>\n" +
                        "            <mes:SystemCode>000</mes:SystemCode>\n" +
                        "            <mes:BankID>01</mes:BankID>\n" +
                        "         </mes:Credentials>\n" +
                        "      </mes:RequestHeader>\n" +
                        "   </soapenv:Header>\n" +
                        "   <soapenv:Body>\n" +
                        "      <bsc:ExchangeRateRequest>\n" +
                        "         <bsc:FromCurrency></bsc:FromCurrency>\n" +
                        "         <bsc:ToCurrency></bsc:ToCurrency>\n" +
                        "         <bsc:RateCode>%s</bsc:RateCode>\n" +
                        "         <bsc:Account></bsc:Account>\n" +
                        "         <bsc:TransactionAmount></bsc:TransactionAmount>\n" +
                        "         <bsc:OperationType>LIST</bsc:OperationType>\n" +
                        "      </bsc:ExchangeRateRequest>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>",
                formattedDate,
                uid,
                UUID.randomUUID().toString(),
                rateCode
        );
    }

    // Helper classes
    private static class ExchangeRateResult {
        private boolean success;
        private double exchangeRate;
        private double convertedAmount;
        private String errorMessage;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public double getExchangeRate() { return exchangeRate; }
        public void setExchangeRate(double exchangeRate) { this.exchangeRate = exchangeRate; }
        public double getConvertedAmount() { return convertedAmount; }
        public void setConvertedAmount(double convertedAmount) { this.convertedAmount = convertedAmount; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    private static class ExchangeRateItem {
        private String fromCurrency;
        private String toCurrency;
        private String rateCode;
        private double exchangeRate;

        // Getters and setters
        public String getFromCurrency() { return fromCurrency; }
        public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }
        public String getToCurrency() { return toCurrency; }
        public void setToCurrency(String toCurrency) { this.toCurrency = toCurrency; }
        public String getRateCode() { return rateCode; }
        public void setRateCode(String rateCode) { this.rateCode = rateCode; }
        public double getExchangeRate() { return exchangeRate; }
        public void setExchangeRate(double exchangeRate) { this.exchangeRate = exchangeRate; }
    }
}