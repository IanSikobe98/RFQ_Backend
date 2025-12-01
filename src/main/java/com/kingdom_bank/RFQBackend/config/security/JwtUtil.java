package com.kingdom_bank.RFQBackend.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.kingdom_bank.RFQBackend.dto.ApiResponse;
import com.kingdom_bank.RFQBackend.entity.User;
import com.kingdom_bank.RFQBackend.entity.UserLoginLog;
import com.kingdom_bank.RFQBackend.enums.ApiResponseCode;
import com.kingdom_bank.RFQBackend.enums.JwtClaims;
import com.kingdom_bank.RFQBackend.repository.UserLoginLogRepo;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtil {
    private final ConstantUtil constantUtil;
    @Value("${jwt.tokenExpiration}")
    private String JWT_TOKEN_EXPIRATION;

    @Value("${jwt.secretKey}")
    private String SECRET_KEY;

    private JWTVerifier jwtVerifier = null;

//    private  final ApiService apiService;

    private final UserLoginLogRepo loginLogRepository;

    public boolean hasAuthorizationBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        return !ObjectUtils.isEmpty(header) && header.startsWith("Bearer");
    }

    public String getJwtToken(HttpServletRequest request) {
        return request.getHeader("Authorization").split(" ")[1].trim();
    }

    public String generateToken(User user) {
        try {
            return JWT.create()
                    .withIssuer("KBRFQ")
                    .withSubject(user.getUsername())
                    .withIssuedAt(new Date())
                    .withExpiresAt(new Date(System.currentTimeMillis() + Long.parseLong(JWT_TOKEN_EXPIRATION)))
                    .withClaim(JwtClaims.USER_ID.getValue(), user.getUserId())
                    .sign(Algorithm.HMAC256(SECRET_KEY));
        } catch (JWTCreationException exception){
            log.info("Invalid Signing configuration / Couldn't convert Claims. Error :: {}", exception.getMessage());
            throw new JWTCreationException(exception.getMessage(), exception);
        }
    }

    public DecodedJWT decodeJwt(String token){
        DecodedJWT decodedJWT;
        try {
            decodedJWT = getJwtVerifier().verify(token);
        } catch (JWTVerificationException exception){
            log.error("JWT TOKEN FOR API USER:{}, IS INVALID :: {}", JWT.decode(token).getSubject(), exception.getMessage());
            throw new JWTVerificationException(exception.getMessage(), exception);
        }

        return decodedJWT;
    }

    private JWTVerifier getJwtVerifier() {
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
        if(jwtVerifier == null){
            jwtVerifier = JWT.require(algorithm)
                    // specify any specific claim validations
                    .withIssuer("COBS")
                    // reusable verifier instance
                    .build();
        }
        return jwtVerifier;
    }

    public boolean validateToken(String token, HttpServletResponse response){
        String userId ="";
        try {
            userId = String.valueOf(JWT.decode(token).getClaim(JwtClaims.USER_ID.getValue()));
            UserLoginLog userLoginLog = loginLogRepository.findDistinctByUser_UserIdAndStatus(Integer.parseInt(userId),constantUtil.ACTIVE);
            if(userLoginLog == null){
                log.error("User Login Logs not found");
                throw  new JWTVerificationException("User Login Logs not found");
            }
            else if(!userLoginLog.getSessionId().equals(token)){
                log.error("Invalid session Id");
                throw  new JWTVerificationException("Invalid session Id");
            }

            getJwtVerifier().verify(token);
            return true;
        }
        catch (JWTVerificationException exception){
            log.error("JWT TOKEN FOR API USER:{}, IS INVALID :: {}", JWT.decode(token).getSubject(), exception.getMessage());
            HashMap<String,String> userInfo = new HashMap<>();
            userInfo.put(JwtClaims.USER_ID.getValue(), userId);
//             ApiResponse apiResponse = apiService.logout(response,false,userInfo);
//             if(apiResponse.getResponseCode().equals(ApiResponseCode.FAIL)){
//                 log.info("System failed to update user log in status with error: {}",apiResponse.getResponseMessage());
//             }
        }
        catch (Exception e) { log.error("Error Occurred During Encryption :: {}", e.getMessage()); }
        return false;
    }

    public Map<String, Claim> getClaims(String token){
        return decodeJwt(token).getClaims();
    }

    public String getSubject(String token) { return decodeJwt(token).getSubject();}

}
