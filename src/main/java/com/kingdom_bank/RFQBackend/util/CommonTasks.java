package com.kingdom_bank.RFQBackend.util;

import com.kingdom_bank.RFQBackend.entity.Status;
import com.kingdom_bank.RFQBackend.repository.StatusRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommonTasks {
    private final StatusRepo statusRepo;
    private static final String SECRET_KEY = "3$RcX@8eWp9Tq3Ls"; // Must match the JS secret key
    private static final String Forex ="FX";
    private static final String kingdomBank ="KB";

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public Status getStatus(int id) {
        return statusRepo.findById(id).orElse(null);
    }

    public String AESdecrypt(String encryptedPassword) throws Exception {
        // Decode the base64-encoded string
        byte[] decodedKey = SECRET_KEY.getBytes("UTF-8");
        SecretKeySpec secretKey = new SecretKeySpec(decodedKey, "AES");

        // Initialize the cipher for decryption
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // Decrypt the password
        byte[] decodedPassword = org.apache.commons.codec.binary.Base64.decodeBase64(encryptedPassword);
        byte[] originalPassword = cipher.doFinal(decodedPassword);
        return new String(originalPassword);
    }

    public String generateOtp(){
        try {

            final int max = 50000;
            final int min = 10000;

            final int ans = (int) (Math.random() * (max - min + 1)) + min;
            return ans + "";
        } catch (final Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public static String cleanPhone(String oldPhoneNumber){
        return "254" + oldPhoneNumber.substring(oldPhoneNumber.length() - 9);
    }

    public static String generateOrderId(String accNo) {
        if (accNo == null || accNo.isBlank()) {
            throw new IllegalArgumentException("CIF Account ID cannot be null or empty");
        }

        String timestamp = LocalDateTime.now().format(FORMATTER);
        return "ORD-" + accNo + "-" + timestamp;
    }


    public static String generateDealCode(String fromCurrency, String toCurrency, String valueDate, Long id) {
        // Define formatter for the input string
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Parse the string to LocalDate
        LocalDate date = LocalDate.parse(valueDate, inputFormatter);

        // Define formatter for the output string
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        // Format the LocalDate to desired output
        String formattedDate = date.format(outputFormatter);

        String padded = String.format("%04d", id);

        return Forex+"-"+kingdomBank+"-"+fromCurrency+toCurrency+"-"+formattedDate+"-"+padded;

    }


}
