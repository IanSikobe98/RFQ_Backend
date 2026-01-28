package com.kingdom_bank.RFQBackend.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingdom_bank.RFQBackend.dto.AuthTokenResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

    @Value("${soa.auth.client_id}")
    private String authClientId;

    @Value("${soa.auth.client_secret}")
    private String clientSecret;

    @Value("${soa.auth.grant_type}")
    private String grantType;

    @Value("${soa.auth.url}")
    private String authUrl;




    public ResponseEntity<String> sendSoaRequest(String service, String soapAction, String endpoint, String request,String logSOARequest) {

        try {
            if(logSOARequest.equalsIgnoreCase("1")) {
                log.info("{} Request {}", service, request);
            }
            ResponseEntity<String> authResponse = authenticate();

            if (authResponse.getStatusCode().is2xxSuccessful()) {
                ObjectMapper objectMapper = new ObjectMapper();
                AuthTokenResponse authTokenResponse = objectMapper.readValue(authResponse.getBody(), AuthTokenResponse.class);


                HttpHeaders headers = new HttpHeaders();

                String encodedAuth = "Bearer " + authTokenResponse.getAccessToken();

                headers.add("Authorization", encodedAuth);
//                headers.add("SOAPAction", "\"" + soapAction + "\"");
                headers.setContentType(MediaType.TEXT_XML);

                HttpEntity<String> sms = new HttpEntity<String>(request, headers);

                return restTemplate.exchange(endpoint, HttpMethod.POST, sms, String.class);
            }
            else{
                log.info("Authentication error occurred while calling service {}", service);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Authentication error occured");
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {

            log.error("Error while sending soa request for {} soapAction {}  endpoint {}", service, soapAction, endpoint, e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error occured while calling " + service + " :" + e.getMessage());
        }
    }


        public ResponseEntity<String> authenticate(){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id",authClientId);
        body.add("client_secret",clientSecret);
        body.add("grant_type",grantType);

        HttpEntity<MultiValueMap<String, String>> requestEntity =
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                authUrl,
                requestEntity,
                String.class
        );
        log.info("Authentication response: {}", response.getBody());
        return response;
    }

}
