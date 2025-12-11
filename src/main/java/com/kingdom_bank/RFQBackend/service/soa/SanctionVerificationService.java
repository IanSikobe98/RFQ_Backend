package com.kingdom_bank.RFQBackend.service.soa;

import com.kingdom_bank.RFQBackend.dto.SanctionVerificationRequest;
import com.kingdom_bank.RFQBackend.util.SoaRequestTemplateUtil;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
public class SanctionVerificationService {
    private final Logger log = LoggerFactory.getLogger(SanctionVerificationService.class);


    @Value("${soa.sanctionVerification.endpoint}")
    private String sanctionVerificationEndpoint;

    private final SoaRequestTemplateUtil soaRequestTemplateUtil;

    public SanctionVerificationService(SoaRequestTemplateUtil soaRequestTemplateUtil) {
        this.soaRequestTemplateUtil = soaRequestTemplateUtil;
    }



    public boolean isCustomerSanctioned(SanctionVerificationRequest request){
        Boolean isCustomerSanctioned = false;
        try {
            ResponseEntity<String> response = soaRequestTemplateUtil.sendSoaRequest("AccountInquiry", "GetAccountDetails", sanctionVerificationEndpoint, sanctionVerificationRequest(request),"1"
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody()!=null) {
                String responseBody = response.getBody();
                if(isJsonArray(responseBody)){
                    JSONArray jsonArray = new JSONArray(responseBody);
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    if(jsonObject!=null && jsonObject.get("result")!=null ){
                        JSONObject result =  jsonObject.getJSONObject("result");
                        if(result.get("decision") !=null ){
                            String decision = result.get("decision").toString();
                            if(decision.equalsIgnoreCase("alert")){
                                isCustomerSanctioned = true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("isCustomerSanctioned:idNumber {}:: Error", request, e);
        }
        log.info(String.valueOf(isCustomerSanctioned));
        return isCustomerSanctioned;
    }



    private String sanctionVerificationRequest(SanctionVerificationRequest verificationRequest) {


        String uid = UUID.randomUUID().toString();

        SimpleDateFormat fd = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date now = new Date();
        String formattedDate = fd.format(now);
        String request = String.format("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mes=\"urn://co-opbank.co.ke/CommonServices/Data/Message/MessageHeader\" xmlns:com=\"urn://co-opbank.co.ke/CommonServices/Data/Common\" xmlns:ns=\"urn://co-opbank.co.ke/TS/ID3/Pep/verifications/Post/1.0\">\n" +
                "   <soapenv:Header>\n" +
                "      <mes:RequestHeader>\n" +
                "         <!--Optional:-->\n" +
                "         <com:CreationTimestamp>%s</com:CreationTimestamp>\n" +
                "         <!--Optional:-->\n" +
                "         <com:CorrelationID>%s</com:CorrelationID>\n" +
                "         <!--Optional:-->\n" +
                "         <mes:FaultTO>?</mes:FaultTO>\n" +
                "         <mes:MessageID>%s</mes:MessageID>\n" +
                "         <!--Optional:-->\n" +
                "         <mes:ReplyTO>?</mes:ReplyTO>\n" +
                "         <!--Optional:-->\n" +
                "         <mes:Credentials>\n" +
                "            <!--Optional:-->\n" +
                "            <mes:SystemCode>COR</mes:SystemCode>\n" +
                "            <!--Optional:-->\n" +
                "            <mes:Username>?</mes:Username>\n" +
                "            <!--Optional:-->\n" +
                "            <mes:Password>?</mes:Password>\n" +
                "            <!--Optional:-->\n" +
                "            <mes:Realm>?</mes:Realm>\n" +
                "            <!--Optional:-->\n" +
                "            <mes:BankID>01</mes:BankID>\n" +
                "         </mes:Credentials>\n" +
                "      </mes:RequestHeader>\n" +
                "   </soapenv:Header>\n" +
                "   <soapenv:Body>\n" +
                "      <ns:PostVerificationsRequest>\n" +
                "         <ns:ProfilesId>%s</ns:ProfilesId>\n" +
                "         <ns:Version>0</ns:Version>\n" +
                "         <ns:FirstName>%s</ns:FirstName>\n" +
                "         <ns:MiddleName>%s</ns:MiddleName>\n" +
                "         <ns:LastNames>%s</ns:LastNames>\n" +
                "         <!--Optional:-->\n" +
                "         <ns:DateOfBirth>%s</ns:DateOfBirth>\n" +
                "         <!--Optional:-->\n" +
                "         <ns:DocumentType>%s</ns:DocumentType>\n" +
                "         <!--Optional:-->\n" +
                "         <ns:DocumentNumber>%s</ns:DocumentNumber>\n" +
                "         <!--Optional:-->\n" +
                "         <ns:Country>Kenya</ns:Country>\n" +
                "      </ns:PostVerificationsRequest>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>", formattedDate, uid, uid,uid, verificationRequest.getFirstName(),
                verificationRequest.getMiddleName(),verificationRequest.getLastName(),verificationRequest.getDob()
                ,verificationRequest.getDocumentType(), verificationRequest.getDocNumber());


        return request;
    }

    private String extractResponseDetail(String key,String response){
        return  StringUtils.substringBetween(response, String.format("<tns25:%s>",key), String.format("</tns25:%s>",key));
    }

    public static boolean isJsonArray(String json) {
        try {
            new JSONArray(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }



}
