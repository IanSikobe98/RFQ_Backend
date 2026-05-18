package com.kingdom_bank.RFQBackend.service.soa;


import com.kingdom_bank.RFQBackend.dto.AccountDetailsDTO;
import com.kingdom_bank.RFQBackend.dto.AccountDetailsResponse;
import com.kingdom_bank.RFQBackend.dto.PostDealCodeResponse;
import com.kingdom_bank.RFQBackend.entity.Order;
import com.kingdom_bank.RFQBackend.enums.ApiResponseCode;
import com.kingdom_bank.RFQBackend.util.SoaRequestTemplateUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostDealCodeService {

    private final Logger log = LoggerFactory.getLogger(PostDealCodeService.class);
    private final SoaRequestTemplateUtil soaRequestTemplateUtil;

    @Value("${soa.postDealCode.endpoint}")
    private String postDealCodeEndpoint;
    @Value("${soa.channel.id}")
    private String channelId;

    public PostDealCodeResponse postDealCode(Order order) {
        PostDealCodeResponse postDealCodeResponse = PostDealCodeResponse.builder().responseCode(ApiResponseCode.FAIL.getCode()).build();
        try {
            String request = buildPostDealCodeRequest(order);
            log.info("BELOW IS THE REQUEST: " + request);
            ResponseEntity<String> response = soaRequestTemplateUtil.sendSoaRequest("PostDealCode", "PostDealCode", postDealCodeEndpoint, request,"1");

            //Save SOA Request
//            var saveSOARequestRes = rfqAPIPayloadService.logRequest(request, channel);

            log.info("Post Deal Code response: {}", response.getBody());

            //Save SOA Response
//            if (saveSOARequestRes != null){
//                Long id = saveSOARequestRes.getId();
//                rfqAPIPayloadService.logResponse(id, response);
//            }

            if (response.getStatusCode().is2xxSuccessful()) {


                String statusCode = StringUtils.substringBetween(response.getBody(), "<ns2:Status>", "</ns2:Status>");
                postDealCodeResponse.setResponseMessage(statusCode);
                if (statusCode != null && statusCode.equalsIgnoreCase("SUCCESS")) {
                    postDealCodeResponse.setResponseCode(ApiResponseCode.SUCCESS.getCode());
                    String message = StringUtils.substringBetween(response.getBody(), "<ns2:Message>", "</ns2:Message>");
                    postDealCodeResponse.setResponseMessage(message);
                }else {
                    String message = StringUtils.substringBetween(response.getBody(), "<ns2:ErrorDesc>", "</ns2:ErrorDesc>");
                    postDealCodeResponse.setResponseMessage(message);
                }

            } else {
                postDealCodeResponse.setResponseMessage("HTTP Error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Post Deal Code Response:: Error  Posting Deal Code with order id of {}", order.getOrderId(), e);
            postDealCodeResponse.setResponseMessage("Error occurred while Posting Deal Code  cif: " + e.getMessage());
        }
        return postDealCodeResponse;
    }


    private String buildPostDealCodeRequest(Order order) {
        String uid = UUID.randomUUID().toString();
        String formattedDate = DateTimeFormatter.ISO_INSTANT
                .format(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        String cif = order.getCifAccountCode() == null? "":order.getCifAccountCode();

        String boughtCurrency = "";
        String soldCurrency = "";

        String boughtAmount = "";
        String soldAmount = "";
        if(order.getBuySell().equalsIgnoreCase("BUY")){
            boughtCurrency = order.getStrongCurrency();
            soldCurrency = order.getWeakCurrency();
        }
        else{
            soldCurrency = order.getStrongCurrency();
            boughtCurrency = order.getWeakCurrency();
        }

        if(boughtCurrency.equalsIgnoreCase(order.getAmountCurrency())){
            boughtAmount = String.valueOf(order.getCounterNominalAmount());
            soldAmount = String.valueOf(order.getExpectedAmount());
        }
        else if(soldCurrency.equalsIgnoreCase(order.getAmountCurrency())){
            soldAmount = String.valueOf(order.getCounterNominalAmount());
            boughtAmount = String.valueOf(order.getExpectedAmount());
        }


        return String.format(
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "   <soapenv:Header>\n" +
                        "      <RequestHeader xmlns=\"https://kingdombankltd.co.ke/banking/core\">\n" +
                        "         <RequestId>%s</RequestId>\n" +
                        "         <ChannelId>%s</ChannelId>\n" +
                        "         <Timestamp>%s</Timestamp>\n" +
                        "      </RequestHeader>\n" +
                        "   </soapenv:Header>\n" +
                        "  <soapenv:Body>\n" +
                        "      <DealInsertRequest xmlns=\"https://kingdombankltd.co.ke/banking/core\">\n" +
                        "         <DealCode>%s</DealCode>\n" +
                        "         <AccountId>%s</AccountId>\n" +
                        "         <CustomerId>%s</CustomerId>\n" +
                        "         <BoughtCurrency>%s</BoughtCurrency>\n" +
                        "         <SoldCurrency>%s</SoldCurrency>\n" +
                        "         <SoldAmount>%s</SoldAmount>\n" +
                        "         <BoughtAmount>%s</BoughtAmount>\n" +
                        "         <OfferRate>%s</OfferRate>\n" +
                        "         <TreaRate>%s</TreaRate>\n" +
                        "         <DealerCode>%s</DealerCode>\n" +
                        "         <Comments>RFQ deal request</Comments>\n" +
                        "      </DealInsertRequest>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>",
                uid,
                channelId,
                formattedDate, order.getDealerCode(),order.getAccountNumber(), cif
                , boughtCurrency,soldCurrency,boughtAmount,soldAmount
                ,order.getNegotiatedRate(),order.getTreasuryCostRate(),order.getTellerId()
        );
    }

}

