package com.kingdom_bank.RFQBackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kingdom_bank.RFQBackend.config.security.SecurityUser;
import com.kingdom_bank.RFQBackend.dto.*;
import com.kingdom_bank.RFQBackend.entity.Role;
import com.kingdom_bank.RFQBackend.entity.Status;
import com.kingdom_bank.RFQBackend.entity.User;
import com.kingdom_bank.RFQBackend.entity.UsersTemp;
import com.kingdom_bank.RFQBackend.enums.ApiResponseCode;
import com.kingdom_bank.RFQBackend.enums.EntityActions;
import com.kingdom_bank.RFQBackend.repository.RoleRepo;
import com.kingdom_bank.RFQBackend.repository.UserRepo;
import com.kingdom_bank.RFQBackend.repository.UserTempRepo;
import com.kingdom_bank.RFQBackend.util.CommonTasks;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.kingdom_bank.RFQBackend.enums.Action.APPROVE;
import static com.kingdom_bank.RFQBackend.enums.Action.REJECT;
import static com.kingdom_bank.RFQBackend.util.CommonTasks.cleanPhone;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;
    private final ConstantUtil constantUtil;
    private final RoleRepo roleRepo;
    private final MailService mailService;
    private final CommonTasks commonTasks;
    private final UserTempRepo userTempRepo;
    @Value("${params.admin_role}")
    private String adminRole;
    private final Environment environment;

    /**
     * Function to get the Authenticated user that was authenticated using JWT
     * @return ApiUser: The authenticated user
     */
    private User getauthenticatedAPIUser(){
        return  ((SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
    }


    public User getUserCredsByUsername(String username) {
        List<Status> statuses = Arrays.asList(constantUtil.ACTIVE);
        return userRepo.findDistinctByUsernameEqualsIgnoreCaseAndStatusIn(username,statuses);
    }

    public ApiResponse createUser(UserRequest request, HttpServletResponse httpServletResponse) {
        log.info("Creating user with request {}", request);
        ApiResponse response = new ApiResponse();
        try {
            User user = getauthenticatedAPIUser();

            List<Status> statuses = Arrays.asList(constantUtil.ACTIVE,constantUtil.PENDING_APPROVAL);
            Optional<User> existingUser = userRepo.findByUsernameEqualsIgnoreCaseAndStatusIn(request.getUserName(),statuses);
            if (existingUser.isPresent()) {
                log.error("User with username {} already exists",request.getUserName());
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage(String.format("User with username %s already exists",request.getUserName()));
                return response;
            }

            request.setPhoneNumber(cleanPhone(request.getPhoneNumber()));
            response = validateUserKYC(request,statuses,false,null);

            //If validation failed return error
            if(response.getResponseCode().equals(ApiResponseCode.FAIL)){
                return response;
            }

            Optional<Role> role = roleRepo.findById(request.getRoleId());
            if (role.isEmpty()) {
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Role of Id  "+ request.getRoleId()+ " does not exist");
                return response;
            }
            Role userRole = role.get();

                UsersTemp newUser = UsersTemp.builder()
                        .username(request.getUserName())
                        .phone(request.getPhoneNumber())
                        .email(request.getEmail())
                        .createdBy(user.getUsername())
                        .dateAdded(new Date())
                        .role(userRole)
                        .action(EntityActions.CREATE.getValue())
                        .status(constantUtil.PENDING_APPROVAL)
                        .build();

                userTempRepo.saveAndFlush(newUser);
                response.setResponseCode(ApiResponseCode.SUCCESS);
                response.setResponseMessage("User successfully created.Awaiting Approval");
        }
        catch (BadCredentialsException e) {
            log.error("ERROR OCCURRED DURING CREATION OF USER:: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Invalid credentials provided by Internal User");
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        }
        catch (Exception e) {
            log.error("ERROR OCCURRED DURING CREATION OF USER:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while creating User");
        }
        return response;
    }
    private ApiResponse validateUserKYC(UserRequest request,List<Status> statuses,boolean isUpdate,User existingUser){
        log.info("Validating User request {}", request);
        ApiResponse response = new ApiResponse();
        try {
            List<User> existingPhoneUser =  userRepo.findByPhoneAndStatusIn(request.getPhoneNumber(), statuses);
            if(!existingPhoneUser.isEmpty()){
                if(isUpdate){
                    if(!existingUser.getPhone().equalsIgnoreCase(request.getPhoneNumber())){
                        log.error("User with phone {} already exists.",request.getPhoneNumber());
                        response.setResponseCode(ApiResponseCode.FAIL);
                        response.setResponseMessage(String.format("User with phone %s already exists...Kindly Use a different Phone Number",request.getPhoneNumber()));
                        return response;
                    }
                }
                else {
                    log.error("User with phone {} already exists.", request.getPhoneNumber());
                    response.setResponseCode(ApiResponseCode.FAIL);
                    response.setResponseMessage(String.format("User with phone %s already exists...Kindly Use a different Phone Number", request.getPhoneNumber()));
                    return response;
                }
            }

            List<User> existingEmailUser =  userRepo.findByEmailEqualsIgnoreCaseAndStatusIn(request.getEmail(), statuses);
            if(!existingEmailUser.isEmpty()){
                if(isUpdate) {
                    if(!existingUser.getEmail().equalsIgnoreCase(request.getEmail())) {
                        log.error("User with email {} already exists.", request.getEmail());
                        response.setResponseCode(ApiResponseCode.FAIL);
                        response.setResponseMessage(String.format("User with email %s already exists...Kindly Use a different Email", request.getEmail()));
                        return response;
                    }
                }
                else {
                    log.error("User with email {} already exists.", request.getEmail());
                    response.setResponseCode(ApiResponseCode.FAIL);
                    response.setResponseMessage(String.format("User with email %s already exists...Kindly Use a different Email", request.getEmail()));
                    return response;
                }
            }

            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("User KYC successfully validated");
            log.info("User KYC successfully validated");
        }
        catch (Exception e) {
            log.error("ERROR OCCURRED DURING CREATION OF USER:: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while creating User");
        }
        return response;
    }

    public ReportResponse getUsers(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<User> usersList = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();

            if (request.getStatuses() != null  && !request.getStatuses().isEmpty()) {
                usersList = userRepo.findByStatus_StatusIdInOrderByDateAddedDesc(request.getStatuses());
            } else {
                usersList = userRepo.findAll(Sort.by(Sort.Direction.DESC, "dateAdded"));
            }


            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Users successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(usersList), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING USERS DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the users Data");
        }
        return response;

    }

    public ReportResponse getUsersPendingApprovals(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<UsersTemp> usersList = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();

            if (request.getStatuses() != null  && !request.getStatuses().isEmpty()) {
                usersList = userTempRepo.findByStatus_StatusIdInOrderByDateAddedDesc(request.getStatuses());
            } else {
                usersList = userTempRepo.findAll(Sort.by(Sort.Direction.DESC, "dateAdded"));
            }


            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Users successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(usersList), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING USERS DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the users Data");
        }
        return response;

    }


    public ApiResponse approveOrRejectUser(ApprovalRequest request, User loggedInUser, Integer id){
        ApiResponse response = new ApiResponse();
        log.info("Approving user of id {}...",id);

        try {
            Optional<UsersTemp> existingUserOptional = userTempRepo.findById(id);
            if (!existingUserOptional.isPresent()) {
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("User  with id  "+ id+ " does not exist");
                return response;
            }
            UsersTemp existingUser = existingUserOptional.get();

            String userRole = loggedInUser.getRole().getRoleName();
            if(!userRole.equalsIgnoreCase(adminRole)) {
                if (existingUser.getCreatedBy().equalsIgnoreCase(loggedInUser.getUsername())) {
                    response.setResponseCode(ApiResponseCode.FAIL);
                    response.setResponseMessage("User cannot approve the user it created");
                    return response;
                }
            }

            UserRequest userRequest = UserRequest.builder().id(id).build();
            userRequest.setComment(request.getDescription());
            if(request.getAction().equals(APPROVE.getValue())){
                if(existingUser.getAction().equalsIgnoreCase(EntityActions.CREATE.getValue())) {
                    User newUser = User.builder()
                            .username(existingUser.getUsername())
                            .phone(existingUser.getPhone())
                            .email(existingUser.getEmail())
                            .createdBy(existingUser.getCreatedBy())
                            .dateAdded(new Date())
                            .approvedBy(loggedInUser.getUsername())
                            .dateApproved(new Date())
                            .role(existingUser.getRole())
                            .status(constantUtil.ACTIVE)
                            .build();
                    userRepo.save(newUser);

                    String subject = "CO-OP BANK SOKO USER REGISTRATION";
                    String msgTemplate = environment.getProperty("msgTemplates.adUserRegistration",
                            "Dear %s, you have been successfully created on the CO-OP BANK SOKO PLATFORM.Kindly use your AD Credentials to Login");
                    String message = String.format(msgTemplate,newUser.getUsername());
                    mailService.sendMail(newUser.getEmail(), subject, message);

                    log.info("User {} create action successfully  approved",existingUser.getId());
                    response.setResponseMessage("Created User successfully Approved.Please login using your AD Credentials to gain Access.");
                }
                else if(existingUser.getAction().equalsIgnoreCase(EntityActions.EDIT.getValue())){
                    User currentUser = existingUser.getUser();
                    currentUser.setUsername(existingUser.getUsername());
                    currentUser.setPhone(existingUser.getPhone());
                    currentUser.setEmail(existingUser.getEmail());
                    currentUser.setRole(existingUser.getRole());
                    currentUser.setUpdatedBy(existingUser.getCreatedBy());
                    currentUser.setDateUpdated(existingUser.getDateApproved());
                    userRepo.save(currentUser);
                    log.info("User {} edit update successfully  approved",existingUser.getId());
                    response.setResponseMessage("User update successfully Approved.");
                }
                else if(existingUser.getAction().equalsIgnoreCase(EntityActions.CHANGE_STATUS.getValue())){
                    User currentUser = existingUser.getUser();
                    currentUser.setStatus(existingUser.getStatus());
                    currentUser.setUpdatedBy(existingUser.getCreatedBy());
                    currentUser.setDateUpdated(existingUser.getDateApproved());
                    userRepo.save(currentUser);
                    log.info("User {} change status update successfully  approved",existingUser.getId());
                    response.setResponseMessage("User update successfully Approved.");
                }
                response.setResponseCode(ApiResponseCode.SUCCESS);
                existingUser.setDateApproved(new Date());
                existingUser.setApprovedBy(loggedInUser.getUsername());
                existingUser.setStatus(constantUtil.ACTIVE);
                userTempRepo.save(existingUser);
            }
            else if(request.getAction().equals(REJECT.getValue())){
                existingUser.setDateApproved(new Date());
                existingUser.setApprovedBy(loggedInUser.getUsername());
                existingUser.setStatus(constantUtil.REJECTED);
                existingUser.setComment(request.getDescription());
                userTempRepo.save(existingUser);

                log.info("User {} successfully  rejected",existingUser.getId());
                response.setResponseMessage("User record successfully Rejected.");
                response.setResponseCode(ApiResponseCode.SUCCESS);
            }
            else{
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("approval action is invalid");
                return response;
            }

        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING APPROVAL OF USER: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred during approval of user");
        }
        return response;
    }



    public ReportResponse getRoles(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<Role> rolesList = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();

            if (request.getStatuses() != null  && !request.getStatuses().isEmpty()) {
                rolesList = roleRepo.findByStatus_StatusIdInOrderByDateAddedDesc(request.getStatuses());
            } else {
                rolesList = roleRepo.findAll(Sort.by(Sort.Direction.DESC, "dateAdded"));
            }


            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Roles successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(rolesList), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING ROLES DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the roles Data");
        }
        return response;

    }

    public ApiResponse changeUserStatus(User user,User loggedInUser , UserRequest request,Boolean isApproval,Optional<User> existingUserNames) {
        log.info("Changing user status  for  request {}", request);
        ApiResponse response = new ApiResponse();
        try {
            if(!isApproval && user.getStatus().equals(constantUtil.PENDING_APPROVAL)){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("User with name "+ request.getUserName()+ " is awaiting approval");
                return response;
            } else if (isApproval && !user.getStatus().equals(constantUtil.PENDING_APPROVAL)) {
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("User with name "+ request.getUserName() + " has already been approved");
                return response;
            }


            if(!isApproval && request.getStatus().equals(constantUtil.ACTIVE.getStatusId()) && existingUserNames.isPresent()){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("User with name "+ request.getUserName() + "  is already active in the system");
                return response;
            }



            Status status = commonTasks.getStatus(request.getStatus());
            if (!Objects.isNull(status)) {
                if(!isApproval){
                    UsersTemp userTemp = UsersTemp.builder()
                            .entityStatus(status.getStatusId())
                            .user(user)
                            .dateAdded(user.getDateAdded())
                            .createdBy(loggedInUser.getUsername())
                            .build();

                    userTempRepo.save(userTemp);
                    log.info("User change status successfully initiated for {}",user.getUsername());

                    response.setResponseCode(ApiResponseCode.SUCCESS);
                    response.setResponseMessage("User status Change Request successfully created");
                }
                else {
                    //APPROVAL OF CREATED USER RECORDS
                    user.setStatus(status);
                    user.setDateApproved(new Date());
                    user.setApprovedBy(loggedInUser.getUsername());
                    userRepo.save(user);

                    response.setResponseCode(ApiResponseCode.SUCCESS);
                    response.setResponseMessage("User Status successfully Approved");
                }

            }
            else{
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("New status does not "+ request.getStatus()+ " does not  exists");
            }
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING Organization  DATA UPDATE:: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while updating the organization");
        }
        return response;
    }






}
