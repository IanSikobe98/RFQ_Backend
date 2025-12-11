package com.kingdom_bank.RFQBackend.util;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class SoaRequestTemplateUtil {

    private final Logger log = LoggerFactory.getLogger(SoaRequestTemplateUtil.class);


    @Value("${soa.sms.username}")
    private String soaUsername;

    private final RestTemplate restTemplate;

    @Value("${soa.sms.password}")
    private String soaPassword;


    public ResponseEntity<String> sendSoaRequest(String service, String soapAction, String endpoint, String request,String logSOARequest) {

        try {
            if(logSOARequest.equalsIgnoreCase("1")) {
                log.info("{} Request {}", service, request);
            }
            HttpHeaders headers = new HttpHeaders();

            String encodedAuth = "Basic " + Base64
                    .getEncoder()
                    .encodeToString(String.format("%s:%s", soaUsername, soaPassword).getBytes());

            headers.add("Authorization", encodedAuth);
            headers.add("SOAPAction", "\"" + soapAction + "\"");
            headers.setContentType(MediaType.TEXT_XML);

            HttpEntity<String> sms = new HttpEntity<String>(request, headers);

            return restTemplate.exchange(endpoint, HttpMethod.POST, sms, String.class);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {

            log.error("Error while sending soa request for {} soapAction {}  endpoint {}", service, soapAction, endpoint, e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error occured while calling " + service + " :" + e.getMessage());
        }
    }

}
