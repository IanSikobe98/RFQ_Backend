package com.kingdom_bank.RFQBackend.controller;

import com.google.gson.Gson;
import com.kingdom_bank.RFQBackend.dto.*;
import com.kingdom_bank.RFQBackend.repository.UserRepo;
import com.kingdom_bank.RFQBackend.service.ApiService;
import com.kingdom_bank.RFQBackend.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final ApiService apiService;
    private final UserService userService;

    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse logout(HttpServletResponse httpServletResponse){
        log.info("LOGOUT  REQUEST ");
        ApiResponse response = apiService.logout(httpServletResponse,true,null);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/create")
    public ApiResponse createUser(HttpServletResponse httpServletResponse ,@RequestBody @Valid UserRequest request){
        log.info("CREATE USER REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = userService.createUser(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/edit")
    public ApiResponse editUser(HttpServletResponse httpServletResponse ,@RequestBody @Valid UserRequest request){
        log.info("EDIT USER REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = userService.editUser(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/read")
    public ApiResponse readUsers(HttpServletResponse httpServletResponse ,@RequestBody @Valid ReportRequest request){
        log.info("READ USERS  REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = userService.getUsers(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }
    @PostMapping("pendingApprovals/read")
    public ApiResponse readUsersPendingApprovals(HttpServletResponse httpServletResponse ,@RequestBody @Valid ReportRequest request){
        log.info("READ USERS  PENDING APPROVALS REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = userService.getUsersPendingApprovals(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/roles/read")
    public ApiResponse readRoles(HttpServletResponse httpServletResponse ,@RequestBody @Valid ReportRequest request){
        log.info("READ ROLES  REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = userService.getRoles(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/roles/create")
    public ApiResponse createRoles(HttpServletResponse httpServletResponse ,@RequestBody @Valid RolesRequest request){
        log.info("CREATE ROLES  REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = userService.createRoles(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }
    @PostMapping("/roles/edit")
    public ApiResponse editRoles(HttpServletResponse httpServletResponse ,@RequestBody @Valid RolesRequest request){
        log.info("EDIT ROLES  REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = userService.editRoles(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/permissions/read")
    public ReportResponse getPermissions(@RequestBody ReportRequest request, HttpServletResponse httpServletResponse) {
        ReportResponse response = new ReportResponse();
        log.info("PERMISSIONS READ  REQUEST :: {}",  new Gson().toJson(request));
        response = userService.getPermissions(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/roles/pendingApprovals/read")
    public ApiResponse readRolesPendingApprovals(HttpServletResponse httpServletResponse ,@RequestBody @Valid ReportRequest request){
        log.info("READ ROLES  PENDING APPROVALS REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = userService.getRolesPendingApprovals(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

}
