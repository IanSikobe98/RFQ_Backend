package com.kingdom_bank.RFQBackend.service;

import com.google.gson.Gson;
import com.kingdom_bank.RFQBackend.config.security.SecurityUser;
import com.kingdom_bank.RFQBackend.dto.ApiResponse;
import com.kingdom_bank.RFQBackend.entity.User;
import com.kingdom_bank.RFQBackend.entity.UserLoginLog;
import com.kingdom_bank.RFQBackend.enums.ApiResponseCode;
import com.kingdom_bank.RFQBackend.enums.JwtClaims;
import com.kingdom_bank.RFQBackend.repository.UserLoginLogRepo;
import com.kingdom_bank.RFQBackend.repository.UserRepo;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiService {

    private final UserRepo userRepository;
    private final ConstantUtil constantUtil;
    private final UserLoginLogRepo userLoginLogRepository;

    /**
     * Function to get the Authenticated user that was authenticated using JWT
     * @return ApiUser: The authenticated user
     */
    private User getauthenticatedAPIUser(){
        return  ((SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
    }


    public ApiResponse logout(HttpServletResponse httpServletResponse, Boolean isAuthenticatedUser, HashMap<String,String> userInfo){
        ApiResponse response = new ApiResponse();
        String logoutDescription = "";
        try{
            User user = new User();
            if(isAuthenticatedUser) {
                user = getauthenticatedAPIUser();
                logoutDescription = "User Account logged out successfully";
            }
            else{
                Integer userId = Integer.valueOf(userInfo.get(JwtClaims.USER_ID.getValue()));
                Optional<User> userOptional = userRepository.findByUserIdAndStatus(userId,constantUtil.ACTIVE);
                if(userOptional.isEmpty()){
                    response.setResponseCode(ApiResponseCode.FAIL);
                    response.setResponseMessage("User not found to logout ");
                }
                user = userOptional.get();
                logoutDescription = "User Account logged out successfully due to token expiry";
            }
            UserLoginLog userLoginLog = userLoginLogRepository.findDistinctByUser_UserIdAndStatus(user.getUserId(),constantUtil.ACTIVE);
            if(userLoginLog == null){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Malicious Activity Detected!");
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                return response;
            }

            userLoginLog.setStatus(constantUtil.INACTIVE);
            userLoginLog.setLogOutTime(new Date());
            userLoginLog.setDescription(logoutDescription);
            userLoginLogRepository.save(userLoginLog);
            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("User Logged Out");
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING LOGIN AUTHENTICATION :: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry, an error occurred while logging out! Please Try again later");
        }
        return response;
    }



}
