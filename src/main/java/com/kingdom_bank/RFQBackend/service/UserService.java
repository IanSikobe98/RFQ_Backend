package com.kingdom_bank.RFQBackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kingdom_bank.RFQBackend.config.security.SecurityUser;
import com.kingdom_bank.RFQBackend.dto.*;
import com.kingdom_bank.RFQBackend.entity.*;
import com.kingdom_bank.RFQBackend.enums.ApiResponseCode;
import com.kingdom_bank.RFQBackend.enums.EntityActions;
import com.kingdom_bank.RFQBackend.repository.*;
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
    private final PrivilegeRepo privilegeRepo;
    private final RoleTempRepo roleTempRepo;
    private final RolePrivilegeRepo rolePrivilegeRepo;
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

    public ApiResponse editUser(UserRequest request, HttpServletResponse httpServletResponse){
        log.info("Editing user with request {}", request);
        ApiResponse response = new ApiResponse();
        try {

            User loggedInUser = getauthenticatedAPIUser();
            log.info("Check if user exists  for id ..... {}",request.getId());
            Optional<User> existingUser = userRepo.findById(request.getId());
            if (!existingUser.isPresent()) {
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("User  selected does not exist");
                return response;
            }

            User user = existingUser.get();
            List<Status> statuses = Arrays.asList(constantUtil.ACTIVE, constantUtil.PENDING_APPROVAL);
            Optional<User> existingUserNames = userRepo.findByUsernameAndStatusIn(request.getUserName(), statuses);

            if (existingUserNames.isPresent() && !user.getUsername().equalsIgnoreCase(request.getUserName())) {
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("User name already exists");
                return response;
            }

            //If it is a status change
            if (!Objects.isNull(request.getStatus())) {
                response = changeUserStatus(user,loggedInUser,request,existingUserNames);
                return response;
            }


            request.setPhoneNumber(cleanPhone(request.getPhoneNumber()));
            response = validateUserKYC(request,statuses,true,user);

            //If validation failed return error
            if(response.getResponseCode().equals(ApiResponseCode.FAIL)){
                return response;
            }

            Optional<Role> roleOpt = roleRepo.findById(request.getRoleId());
            if (roleOpt.isEmpty()) {
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Role selected does not exist");
                log.info("Role of Id  "+ request.getRoleId()+ " does not exist");
                return response;
            }

            Role role = roleOpt.get();


            UsersTemp usersTemp = UsersTemp.builder()
                    .user(user)
                    .username(request.getUserName())
                    .phone(request.getPhoneNumber())
                    .email(request.getEmail())
                    .role(role)
                    .createdBy(loggedInUser.getUsername())
                    .dateAdded(new Date())
                    .action(EntityActions.EDIT.getValue())
                    .status(constantUtil.PENDING_APPROVAL)
                    .build();

            userTempRepo.saveAndFlush(usersTemp);
            log.info("User Update Request created successfully id {}",usersTemp.getId());
            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("User Update Request created successfully");
        }

        catch (Exception e) {
        log.error("ERROR OCCURRED DURING UPDATING OF USERS:: {}" ,e.getMessage());
        e.printStackTrace();
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        response.setResponseCode(ApiResponseCode.FAIL);
        response.setResponseMessage("Sorry,Error occurred while updating users");
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


            usersList.stream().forEach(user ->{
                if(user.getAction().equalsIgnoreCase(EntityActions.CHANGE_STATUS.getValue())){
                    Status status = commonTasks.getStatus(user.getEntityStatus());
                    if(!Objects.isNull(status)){
                        user.setEntityStatusName(status.getStatusName());
                        user.setUsername(user.getUser().getUsername());
                        user.setPhone(user.getUser().getPhone());
                        user.setEmail(user.getUser().getEmail());
//                        user.setRole(user.getUser().getRole());
                    }
                }
            });


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

//            String userRole = loggedInUser.getRole().getRoleName();
//            if(!userRole.equalsIgnoreCase(adminRole)) {
//                if (existingUser.getCreatedBy().equalsIgnoreCase(loggedInUser.getUsername())) {
//                    response.setResponseCode(ApiResponseCode.FAIL);
//                    response.setResponseMessage("User cannot approve the user it created");
//                    return response;
//                }
//            }

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
                    if(existingUser.getUsername()!=null && !existingUser.getUsername().isEmpty()){
                        currentUser.setUsername(existingUser.getUsername());
                    }
                    if(existingUser.getPhone()!=null && !existingUser.getPhone().isEmpty()){
                        currentUser.setPhone(existingUser.getPhone());
                    }
                    if(existingUser.getEmail()!=null && !existingUser.getEmail().isEmpty()){
                        currentUser.setEmail(existingUser.getEmail());
                    }
                    if(existingUser.getRole()!=null ){
                        currentUser.setRole(existingUser.getRole());
                    }

                    currentUser.setUpdatedBy(existingUser.getCreatedBy());
                    currentUser.setDateUpdated(existingUser.getDateApproved());
                    userRepo.save(currentUser);
                    log.info("User {} edit update successfully  approved",existingUser.getId());
                    response.setResponseMessage("User update successfully Approved.");
                }
                else if(existingUser.getAction().equalsIgnoreCase(EntityActions.CHANGE_STATUS.getValue())){

                    Status status = commonTasks.getStatus(existingUser.getEntityStatus());
                    if(Objects.isNull(status)){
                        response.setResponseCode(ApiResponseCode.FAIL);
                        response.setResponseMessage("Status for role not found");
                        return response;
                    }
                    User currentUser = existingUser.getUser();
                    currentUser.setStatus(status);
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



            rolesList.forEach(role -> {
                List<PermsDto> permsDtoList = new ArrayList<>();
                List<RolePrivilege> rolePrivileges = rolePrivilegeRepo.findByRole_RoleId(role.getRoleId());
                if(!rolePrivileges.isEmpty()){
                    rolePrivileges.forEach(rolePrivilege -> {
                        PermsDto permsDto = PermsDto.builder()
                                .id(rolePrivilege.getPrivilege().getPrivilegeId())
                                .permission(rolePrivilege.getPrivilege().getPrivilegeName())
                                .build();
                        permsDtoList.add(permsDto);
                    });
                }
                role.setPrivilegeList(permsDtoList);
            });


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

    public ApiResponse changeUserStatus(User user,User loggedInUser , UserRequest request,Optional<User> existingUserNames) {
        log.info("Changing user status  for  request {}", request);
        ApiResponse response = new ApiResponse();
        try {
            if(user.getStatus().equals(constantUtil.PENDING_APPROVAL)){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("User with name "+ request.getUserName()+ " is awaiting approval");
                return response;
            }

            if(request.getStatus().equals(constantUtil.ACTIVE.getStatusId()) && existingUserNames.isPresent()){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("User with name "+ request.getUserName() + "  is already active in the system");
                return response;
            }



            Status status = commonTasks.getStatus(request.getStatus());
            if (!Objects.isNull(status)) {
                    UsersTemp userTemp = UsersTemp.builder()
                            .entityStatus(status.getStatusId())
                            .status(constantUtil.PENDING_APPROVAL)
                            .user(user)
                            .action(EntityActions.CHANGE_STATUS.getValue())
                            .dateAdded(new Date())
                            .createdBy(loggedInUser.getUsername())
                            .build();

                    userTempRepo.save(userTemp);
                    log.info("User change status successfully initiated for {}",user.getUsername());

                    response.setResponseCode(ApiResponseCode.SUCCESS);
                    response.setResponseMessage("User status Change Request successfully created");
            }
            else{
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("New status does not "+ request.getStatus()+ " does not  exists");
            }
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING User DATA UPDATE:: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while updating the user");
        }
        return response;
    }

    public ApiResponse createRoles(RolesRequest request, HttpServletResponse httpServletResponse){
        log.info("Creating role  with request {}", request);
        ApiResponse response = new ApiResponse();
        try{
            User user = getauthenticatedAPIUser();
            List<Status> statuses = Arrays.asList(constantUtil.ACTIVE,constantUtil.PENDING_APPROVAL);
            List<Role> existingRoles = roleRepo.findByRoleNameAndStatusIn(request.getName(),statuses);
            List<Integer> invalidPermsList = new ArrayList<>();
            if (!existingRoles.isEmpty()) {
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Role with name "+ request.getName()+ " already exists");
                return response;
            }

            if(request.getPermissions()!=null && !request.getPermissions().isEmpty()) {
                request.getPermissions().stream().forEach(permissionId -> {
                   Privilege privilege = privilegeRepo.findByPrivilegeIdAndStatus(Integer.valueOf(permissionId), constantUtil.ACTIVE);
                    if (Objects.isNull(privilege)) {
                        //Invalid permissions attached
                        invalidPermsList.add(Integer.valueOf(permissionId));
                    }
                });
                if (invalidPermsList.isEmpty()) {
                    String joinedPermissions = String.join(",", request.getPermissions());
                    RolesTemp rolesTemp = RolesTemp.builder()
                            .roleName(request.getName())
                            .roleDescription(request.getDescription())
                            .permissions(joinedPermissions)
                            .dateAdded(new Date())
                            .createdBy(user.getUsername())
                            .action(EntityActions.CREATE.getValue())
                            .status(constantUtil.PENDING_APPROVAL)
                            .build();

                    roleTempRepo.save(rolesTemp);

                    response.setResponseCode(ApiResponseCode.SUCCESS);
                    response.setResponseMessage("Roles successfully created.Awaiting Approval");
                }
                else{
                    response.setResponseCode(ApiResponseCode.FAIL);
                    response.setResponseMessage("Permisssions selected do not exist or are not active");
                    return response;
                }
            }
            else{
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Role with name "+ request.getName()+ " has not mapped any permissions");
                return response;
            }

            }
        catch (Exception e) {
            log.error("ERROR OCCURRED DURING CREATION OF ROLES:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while creating roles");
        }
        return response;
    }


    public ApiResponse editRoles(RolesRequest request, HttpServletResponse httpServletResponse){
        log.info("Editing roles with request {}", request);
        ApiResponse response = new ApiResponse();
        try {
            List<Integer> invalidPermsList = new ArrayList<>();
            User loggedInUser = getauthenticatedAPIUser();
            log.info("Check if role exists  for id ..... {}",request.getId());
            Optional<Role> existingRole = roleRepo.findById(request.getId());
            if (!existingRole.isPresent()) {
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Role  selected does not exist");
                return response;
            }

            Role role = existingRole.get();

            List<Status> statuses = Arrays.asList(constantUtil.ACTIVE,constantUtil.PENDING_APPROVAL);
            List<Role> existingRoleNames = roleRepo.findByRoleNameAndStatusIn(request.getName(),statuses);

            if (!existingRoleNames.isEmpty() && !role.getRoleName().equalsIgnoreCase(request.getName())) {
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Role name already exists");
                return response;
            }
            //If it is a status change
            if (!Objects.isNull(request.getStatus())) {
                response = changeRoleStatus(role,loggedInUser,request);
                return response;
            }



            if(request.getPermissions()!=null && !request.getPermissions().isEmpty()){
                request.getPermissions().stream().forEach(permissionId -> {
                    Privilege privilege = privilegeRepo.findByPrivilegeIdAndStatus(Integer.valueOf(permissionId), constantUtil.ACTIVE);
                    if (Objects.isNull(privilege)) {
                        //Invalid permissions attached
                        invalidPermsList.add(Integer.valueOf(permissionId));
                    }
                });
                if (invalidPermsList.isEmpty()) {
                    String joinedPermissions = String.join(",", request.getPermissions());
                    RolesTemp rolesTemp = RolesTemp.builder()
                            .role(role)
                            .roleName(request.getName())
                            .roleDescription(request.getDescription())
                            .permissions(joinedPermissions)
                            .createdBy(loggedInUser.getUsername())
                            .dateAdded(new Date())
                            .action(EntityActions.EDIT.getValue())
                            .status(constantUtil.PENDING_APPROVAL)
                            .build();

                    roleTempRepo.saveAndFlush(rolesTemp);
                    log.info("Roles Update Request created successfully id {}",rolesTemp.getId());
                    response.setResponseCode(ApiResponseCode.SUCCESS);
                    response.setResponseMessage("Roles Update Request created successfully");
                    return response;
                }
                else{
                    response.setResponseCode(ApiResponseCode.FAIL);
                    response.setResponseMessage("Permisssions selected do not exist or are not active");
                    return response;
                }
            }
            else{
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Role with name "+ request.getName()+ " has not mapped any permissions");
                return response;
            }
        }
        catch (Exception e) {
            log.error("ERROR OCCURRED DURING UPDATING OF USERS:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while updating users");
        }
        return response;
    }


    public ApiResponse changeRoleStatus(Role role,User loggedInUser , RolesRequest request) {
        log.info("Changing role status  for  request {}", request);
        ApiResponse response = new ApiResponse();
        try {
            if(role.getStatus().equals(constantUtil.PENDING_APPROVAL)){
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Role with name "+ request.getName()+ " is awaiting approval");
                return response;
            }



            Status status = commonTasks.getStatus(request.getStatus());
            if (!Objects.isNull(status)) {
                RolesTemp userTemp = RolesTemp.builder()
                        .entityStatus(status.getStatusId())
                        .status(constantUtil.PENDING_APPROVAL)
                        .role(role)
                        .action(EntityActions.CHANGE_STATUS.getValue())
                        .dateAdded(new Date())
                        .createdBy(loggedInUser.getUsername())
                        .build();

                roleTempRepo.save(userTemp);
                log.info("Role change status successfully initiated for {}",role.getRoleName());

                response.setResponseCode(ApiResponseCode.SUCCESS);
                response.setResponseMessage("Role status Change Request successfully created");
            }
            else{
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("New status does not "+ request.getStatus()+ " does not  exists");
            }
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING Role  DATA UPDATE:: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while updating the role");
        }
        return response;
    }
    public ApiResponse approveOrRejectRole(ApprovalRequest request, User loggedInUser, Integer id){
        ApiResponse response = new ApiResponse();
        log.info("Approving role of id {}...",id);

        try {
            Optional<RolesTemp> existingRoleOptional = roleTempRepo.findById(id);
            if (!existingRoleOptional.isPresent()) {
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Role  with id  "+ id+ " does not exist");
                return response;
            }
            RolesTemp existingRole = existingRoleOptional.get();

//            String userRole = loggedInUser.getRole().getRoleName();
//            if(!userRole.equalsIgnoreCase(adminRole)) {
//                if (existingRole.getCreatedBy().equalsIgnoreCase(loggedInUser.getUsername())) {
//                    response.setResponseCode(ApiResponseCode.FAIL);
//                    response.setResponseMessage("User cannot approve the role it created");
//                    return response;
//                }
//            }

            UserRequest userRequest = UserRequest.builder().id(id).build();
            userRequest.setComment(request.getDescription());
            if(request.getAction().equals(APPROVE.getValue())){
                if(existingRole.getAction().equalsIgnoreCase(EntityActions.CREATE.getValue())) {
                    Role newRole = Role.builder()
                            .roleName(existingRole.getRoleName())
                            .roleDescription(existingRole.getRoleDescription())
                            .createdBy(existingRole.getCreatedBy())
                            .dateAdded(new Date())
                            .approvedBy(loggedInUser.getUsername())
                            .dateApproved(new Date())
                            .status(constantUtil.ACTIVE)
                            .build();
                    roleRepo.save(newRole);
                    log.info("Role created successfully for {}",existingRole.getRoleName());

                    String permissionsStr = existingRole.getPermissions();
                    List<String> permissions = Arrays.asList(permissionsStr.split(","));
                    List<RolePrivilege> newPrivilegeList = new ArrayList<>();
                    permissions.forEach(permission -> {
                        Privilege privilege = privilegeRepo.findByPrivilegeIdAndStatus(Integer.valueOf(permission), constantUtil.ACTIVE);
                        if(!Objects.isNull(privilege)) {
                            RolePrivilege rolePrivilege = RolePrivilege.builder()
                                    .role(newRole)
                                    .privilege(privilege)
                                    .createdBy(existingRole.getCreatedBy())
                                    .privilege(privilege)
                                    .dateAdded(new Date())
                                    .dateApproved(new Date())
                                    .approvedBy(loggedInUser.getUsername())
                                    .build();
                            newPrivilegeList.add(rolePrivilege);
                        }
                    });
                    rolePrivilegeRepo.saveAll(newPrivilegeList);

                    log.info("Permissions successfully mapped for {}",existingRole.getRoleName());
                    log.info("Role {} create action successfully  approved",existingRole.getId());
                    response.setResponseMessage("Created Role successfully Approved.");
                }
                else if(existingRole.getAction().equalsIgnoreCase(EntityActions.EDIT.getValue())){
                    Role currentRole = existingRole.getRole();

                    if(existingRole.getRoleName()!=null && !existingRole.getRoleName().isEmpty()){
                        currentRole.setRoleName(existingRole.getRoleName());
                    }
                    if(existingRole.getRoleDescription()!=null && !existingRole.getRoleDescription().isEmpty()){
                        currentRole.setRoleDescription(existingRole.getRoleDescription());
                    }

                    currentRole.setUpdatedBy(existingRole.getCreatedBy());
                    currentRole.setDateUpdated(existingRole.getDateApproved());
                    roleRepo.save(currentRole);

                    List<RolePrivilege> newPrivilegeList = new ArrayList<>();
                    String permissionsStr = existingRole.getPermissions();
                    List<String> permissions = Arrays.asList(permissionsStr.split(","));
                    if(!permissions.isEmpty()){
                        List<RolePrivilege> currentPrivileges =  rolePrivilegeRepo.findByRole_RoleId(existingRole.getId());
                        if(!currentPrivileges.isEmpty()){
                            rolePrivilegeRepo.deleteAll(currentPrivileges);
                        }
                        log.info("Permissions mapping deleted successfully.......");

                        permissions.forEach(permission -> {
                           Privilege privilege =  privilegeRepo.findByPrivilegeIdAndStatus(Integer.valueOf(permission), constantUtil.ACTIVE);
                           if(!Objects.isNull(privilege)) {
                               RolePrivilege rolePrivilege = RolePrivilege.builder()
                                       .role(currentRole)
                                       .privilege(privilege)
                                       .createdBy(existingRole.getCreatedBy())
                                       .dateAdded(new Date())
                                       .dateApproved(new Date())
                                       .approvedBy(loggedInUser.getUsername())
                                       .build();
                               newPrivilegeList.add(rolePrivilege);
                           }
                        });
                        rolePrivilegeRepo.saveAll(newPrivilegeList);
                    }
                    log.info("Role {} edit update successfully  approved",existingRole.getId());
                    response.setResponseMessage("Role update successfully Approved.");
                }
                else if(existingRole.getAction().equalsIgnoreCase(EntityActions.CHANGE_STATUS.getValue())){
                    Role currentRole = existingRole.getRole();
                    Status status = commonTasks.getStatus(existingRole.getEntityStatus());
                    if(Objects.isNull(status)){
                        response.setResponseCode(ApiResponseCode.FAIL);
                        response.setResponseMessage("Status for role not found");
                        return response;
                    }
                    currentRole.setStatus(status);
                    currentRole.setUpdatedBy(existingRole.getCreatedBy());
                    currentRole.setDateUpdated(existingRole.getDateApproved());
                    roleRepo.save(currentRole);
                    log.info("Role {} change status update successfully  approved",existingRole.getId());
                    response.setResponseMessage("User update successfully Approved.");
                }
                response.setResponseCode(ApiResponseCode.SUCCESS);
                existingRole.setDateApproved(new Date());
                existingRole.setApprovedBy(loggedInUser.getUsername());
                existingRole.setStatus(constantUtil.ACTIVE);
                roleTempRepo.save(existingRole);
            }
            else if(request.getAction().equals(REJECT.getValue())){
                existingRole.setDateApproved(new Date());
                existingRole.setApprovedBy(loggedInUser.getUsername());
                existingRole.setStatus(constantUtil.REJECTED);
                existingRole.setComment(request.getDescription());
                roleTempRepo.save(existingRole);

                log.info("Role {} successfully  rejected",existingRole.getId());
                response.setResponseMessage("Role record successfully Rejected.");
                response.setResponseCode(ApiResponseCode.SUCCESS);
            }
            else{
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("approval action is invalid");
                return response;
            }

        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING APPROVAL OF ROLE: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred during approval of ROLE");
        }
        return response;
    }

    public ReportResponse getPermissions(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<Privilege> privilegesList = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();

            if (request.getStatuses() != null  && !request.getStatuses().isEmpty()) {
                privilegesList = privilegeRepo.findByStatus_StatusIdInOrderByDateAddedDesc(request.getStatuses());
            } else {
                privilegesList = privilegeRepo.findAll(Sort.by(Sort.Direction.DESC, "dateAdded"));
            }


            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Privileges successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(privilegesList), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING PRIVILEGES DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the privileges Data");
        }
        return response;

    }

    public ReportResponse getPermissionsByRole(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<RolePrivilege> privilegesList = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();

            privilegesList = rolePrivilegeRepo.findByRole_RoleId(Integer.valueOf(request.getId()));


            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Privileges successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(privilegesList), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING PRIVILEGES DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the privileges Data");
        }
        return response;

    }


    public ReportResponse getRolesPendingApprovals(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<RolesTemp> rolesList = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();

            if (request.getStatuses() != null  && !request.getStatuses().isEmpty()) {
                rolesList = roleTempRepo.findByStatus_StatusIdInOrderByDateAddedDesc(request.getStatuses());
            } else {
                rolesList = roleTempRepo.findAll(Sort.by(Sort.Direction.DESC, "dateAdded"));
            }

            rolesList.stream().forEach(role ->{
                List<PermsDto> permsDtoList = new ArrayList<>();
                String permissionsStr = role.getPermissions();
                if(permissionsStr!=null) {
                    List<String> permissions = Arrays.asList(permissionsStr.split(","));
                    if (!permissions.isEmpty()) {
                        permissions.stream().forEach(permission -> {
                            Privilege privilege = privilegeRepo.findByPrivilegeIdAndStatus(Integer.valueOf(permission), constantUtil.ACTIVE);
                            if (privilege != null) {
                                PermsDto permsDto = PermsDto.builder()
                                        .id(privilege.getPrivilegeId())
                                        .permission(privilege.getPrivilegeName())
                                        .build();
                                permsDtoList.add(permsDto);
                            }
                        });
                        role.setPrivilegeList(permsDtoList);
                    }
                }
                if(role.getAction().equalsIgnoreCase(EntityActions.CHANGE_STATUS.getValue())){
                    Status status = commonTasks.getStatus(role.getEntityStatus());
                    if(!Objects.isNull(status)){
                        role.setEntityStatusName(status.getStatusName());
                        role.setRoleName(role.getRole().getRoleName());
                        role.setRoleDescription(role.getRole().getRoleDescription());
                    }
                }
            });


            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("roles successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(rolesList), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING PRIVILEGES DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the privileges Data");
        }
        return response;

    }









}
