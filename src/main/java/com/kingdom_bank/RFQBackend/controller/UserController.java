package com.kingdom_bank.RFQBackend.controller;

import com.google.gson.Gson;
import com.kingdom_bank.RFQBackend.dto.ApiResponse;
import com.kingdom_bank.RFQBackend.dto.ReportRequest;
import com.kingdom_bank.RFQBackend.dto.UserRequest;
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

}
