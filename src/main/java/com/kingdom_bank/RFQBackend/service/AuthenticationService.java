package com.kingdom_bank.RFQBackend.service;

import com.kingdom_bank.RFQBackend.config.security.JwtUtil;
import com.kingdom_bank.RFQBackend.dto.*;
import com.kingdom_bank.RFQBackend.entity.RolePrivilege;
import com.kingdom_bank.RFQBackend.entity.Status;
import com.kingdom_bank.RFQBackend.entity.User;
import com.kingdom_bank.RFQBackend.entity.UserLoginLog;
import com.kingdom_bank.RFQBackend.enums.ApiResponseCode;
import com.kingdom_bank.RFQBackend.repository.RolePrivilegeRepo;
import com.kingdom_bank.RFQBackend.repository.UserLoginLogRepo;
import com.kingdom_bank.RFQBackend.util.CommonTasks;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;

@Service
@Slf4j
@Validated
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authManager;
    private final CommonTasks commonTasks;
    private final UserService userService;
    private final ConstantUtil constantUtil;
    private final UserLoginLogRepo userLoginLogRepo;
    private final Environment environment;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final JwtUtil jwtUtil;
    private final RolePrivilegeRepo rolePrivilegeRepo;

    @Value("${soa.sms.retries}")
    private String smsRetries;

    public AuthResponse authenticate(AuthRequest authRequest, HttpServletResponse httpServletResponse){
        AuthResponse response = new AuthResponse();
        UserLoginLog userLoginLog = new UserLoginLog();

        try{
            String username = authRequest.getUsername();
            String password = commonTasks.AESdecrypt(authRequest.getPassword());

            //Check if user exist
            User user = userService.getUserCredsByUsername(username);

            if(Objects.isNull(user)){
                log.error("User Does not exist :: {}",username);
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Invalid Credentials");
                log.info("User does not exist");
                return response;
            }

            List<Status> statuses = Arrays.asList(constantUtil.ACTIVE,constantUtil.PENDING);
            //Check is user is logged in
            userLoginLog = userLoginLogRepo.findDistinctByUser_UserIdAndStatusIn(user.getUserId(),statuses);

            if(!Objects.isNull(userLoginLog)){
                userLoginLog.setStatus(constantUtil.INACTIVE);
                userLoginLogRepo.save(userLoginLog);
            }


            userLoginLog = UserLoginLog.builder().user(user).authStatus(constantUtil.PENDING)
                    .status(constantUtil.PENDING).build();

            UsernamePasswordAuthenticationToken userAuthentication =
                    new UsernamePasswordAuthenticationToken(username, password);

            //Authenticate using LDAP or AD credentials
            Authentication authentication = authManager.authenticate(userAuthentication);

            if (authentication.isAuthenticated()) {
                log.info("User successfully authenticated using LDAP");
            }

            //Generate Otp and send otp
            boolean isStaticOtp = Boolean.parseBoolean(environment.getProperty("rfq.app.isStaticOtp", "false"));
            log.info("is Static OTP {}",isStaticOtp);
            String otp = "";
            if(isStaticOtp){
                otp =environment.getProperty("rfq.app.staticOtpValue","70000");
            }
            else {
                otp = commonTasks.generateOtp();
            }
            userLoginLog.setOtp(passwordEncoder.encode(otp));
            userLoginLog.setOtpExpiryTime(calculateOtpExpiryDate());

            boolean sendSms = false;
            if(isStaticOtp){
                sendSms = true;
            }
            else {
                int retryCounter = 0;
                int totalRetries = Integer.parseInt(smsRetries);
                while (retryCounter < totalRetries) {
                    String message = "The otp is " + otp;

                    log.info("Attempt {} to Send otp  to phone {} ", retryCounter + 1, user.getPhone());
//                    sendSms = smsNotificationService.sendSMS(userCreds.getUser().getPhone(), message);
                    sendSms = true;
                    String subject = "CO-OP BANK SOKO OTP";
                    mailService.sendMail(user.getEmail(), subject, message);

                    if (sendSms) {
                        break;
                    }
                    retryCounter++;
                }
            }
            if(!sendSms){
                userLoginLog.setDescription("Failed to send OTP");
                userLoginLog.setAuthStatus(constantUtil.FAILED);
                userLoginLog.setCreateDate(new Date());
                userLoginLogRepo.save(userLoginLog);
            }
            else{
                userLoginLog.setDescription("Successful Authentication");
                userLoginLog.setAuthStatus(constantUtil.OTP_SENT);
                userLoginLog.setCreateDate(new Date());
                userLoginLogRepo.save(userLoginLog);
            }

            //TODO --> Confirm if otp fails if it should return failure  in the process
            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Authentication Successful");

            log.info("USERNAME:{} AUTHENTICATED SUCCESSFULLY", user.getUsername());
        }
        catch (UsernameNotFoundException e){
            //UsernameNotFoundException occurs when the username is invalid
            log.error("UsernameNotFound :: {}", e.getMessage());
            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Invalid Username or Password!");
        }
        catch (BadCredentialsException e){
            //BadCredentialsException occurs when the password is invalid
            userLoginLog.setDescription("Invalid Password");
            log.info("Invalid Password");
            userLoginLog.setAuthStatus(constantUtil.FAILED);
            userLoginLog.setStatus(constantUtil.FAILED);
            userLoginLog.setCreateDate(new Date());
            userLoginLog.setStatus(constantUtil.FAILED);
            userLoginLogRepo.save(userLoginLog);

            log.error("BadCredentialsException :: {}", e.getMessage());
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Invalid Username or Password!");

        }
        catch(DisabledException e){
            //User is deactivated
            userLoginLog.setDescription("User is deactivated");
            userLoginLog.setAuthStatus(constantUtil.FAILED);
            userLoginLog.setStatus(constantUtil.FAILED);
            userLoginLog.setCreateDate(new Date());
            userLoginLogRepo.save(userLoginLog);


            log.error("DisabledException :: {}", e.getMessage());
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Your API Credentials have been deactivated!.");
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING LOGIN AUTHENTICATION :: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry, an authentication Error has occurred! Please Try again later");
        }

        return response;
    }

    public OtpResponse validateOtp(OtpRequest otpRequest, HttpServletResponse httpServletResponse) {
        OtpResponse response = new OtpResponse();
        UserLoginLog userLoginLog = new UserLoginLog();
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        try{

            String username = otpRequest.getUsername();
            String password = commonTasks.AESdecrypt(otpRequest.getPassword());
            String otp = otpRequest.getOtp();


            //Check if user exist
            User user = userService.getUserCredsByUsername(username);

            if(Objects.isNull(user)){
                log.error("User Does not exist :: {}",username);
                httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("User does not exist");
                return response;
            }

            userLoginLog = userLoginLogRepo.findDistinctByUser_UserIdAndStatus(user.getUserId(),constantUtil.PENDING);

            if(Objects.isNull(userLoginLog)){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Malicious Activity Detected!");
                httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return response;
            }


            UsernamePasswordAuthenticationToken userAuthentication =
                    new UsernamePasswordAuthenticationToken(username, password);

            //Authenticate
            Authentication authentication = authManager.authenticate(userAuthentication);

            if (authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                    // If authenticated via LDAP
                    log.info("User successfully authenticated using LDAP");
            }


            if(userLoginLog.getOtpExpiryTime().before(new Date())){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("OTP has Expired");
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                userLoginLog.setOtpAuthStatus(constantUtil.FAILED);
                userLoginLog.setDescription("OTP has Expired");
                userLoginLog.setStatus(constantUtil.INACTIVE);
                userLoginLogRepo.save(userLoginLog);
                return response;
            }

            //validate OTP
            if(!passwordEncoder.matches(otp,userLoginLog.getOtp())){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("wrong OTP Provided");
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);

                userLoginLog.setOtpAuthStatus(constantUtil.FAILED);
                userLoginLog.setDescription("wrong OTP Provided");
                userLoginLogRepo.save(userLoginLog);
                return response;
            }

            String token = jwtUtil.generateToken(user);
            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage(user.getUsername()+" signed in successfully");
            response.setToken(token);
            userInfoDTO.setUser(user);

            List<RolePrivilege> usersPerms =  rolePrivilegeRepo.findByRole_RoleId(user.getRole().getRoleId());
            List<String> userPermsArray = new ArrayList<>();
            usersPerms.stream().forEach(usersPerm -> {
                userPermsArray.add(usersPerm.getPrivilege().getPrivilegeName());
            });

            userInfoDTO.setRole(user.getRole().getRoleName());
            if(!userPermsArray.isEmpty()){
                userInfoDTO.setUsersPerm(userPermsArray);
            }
            response.setUser(userInfoDTO);

            userLoginLog.setOtpAuthStatus(constantUtil.SUCCESS);
            userLoginLog.setSessionStartTime(new Date());
            userLoginLog.setSessionId(token);
            userLoginLog.setStatus(constantUtil.ACTIVE);
            userLoginLog.setDescription("OTP validated Successfully");
            userLoginLogRepo.save(userLoginLog);

        }
        catch (UsernameNotFoundException e){
            //UsernameNotFoundException occurs when the username is invalid
            log.error("UsernameNotFound :: {}", e.getMessage());
//            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Invalid Username or Password!");
        }
        catch (BadCredentialsException e){
            //BadCredentialsException occurs when the password is invalid
            userLoginLog.setDescription("Invalid Password");
            userLoginLog.setAuthStatus(constantUtil.FAILED);
            userLoginLog.setCreateDate(new Date());
            userLoginLogRepo.save(userLoginLog);

            log.error("BadCredentialsException :: {}", e.getMessage());
//            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Invalid Username or Password!");

        }
        catch(DisabledException e){
            //User is deactivated
            userLoginLog.setDescription("User is deactivated");
            userLoginLog.setAuthStatus(constantUtil.FAILED);
            userLoginLog.setCreateDate(new Date());
            userLoginLogRepo.save(userLoginLog);

            httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            log.error("DisabledException :: {}", e.getMessage());
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Your API Credentials have been deactivated!.");
        }
        catch(Exception e){
            log.error("ERROR OCCURRED DURING LOGIN AUTHENTICATION :: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry, an authentication Error has occurred! Please Try again later");
        }
        return response;
    }


    private Date calculateOtpExpiryDate(){
        Date now = new Date();

        // Use Calendar to add 5 minutes to the current time
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.MINUTE, 5);

        // Get the updated time
        return calendar.getTime();
    }
}
