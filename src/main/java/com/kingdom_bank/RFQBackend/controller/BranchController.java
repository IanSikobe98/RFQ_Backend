package com.kingdom_bank.RFQBackend.controller;


import com.google.gson.Gson;
import com.kingdom_bank.RFQBackend.dto.ApiResponse;
import com.kingdom_bank.RFQBackend.dto.BranchRequest;
import com.kingdom_bank.RFQBackend.dto.ReportRequest;
import com.kingdom_bank.RFQBackend.dto.UserRequest;
import com.kingdom_bank.RFQBackend.service.BranchService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/branch")
@RequiredArgsConstructor
@Slf4j
public class BranchController {

    private final BranchService branchService;

    @PostMapping("/create")
    public ApiResponse createBranch(HttpServletResponse httpServletResponse , @RequestBody @Valid BranchRequest request){
        log.info("CREATE BRANCH REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = branchService.createBranch(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/edit")
    public ApiResponse editBranch(HttpServletResponse httpServletResponse ,@RequestBody @Valid BranchRequest request){
        log.info("EDIT BRANCH REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = branchService.editBranch(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/read")
    public ApiResponse readBranches(HttpServletResponse httpServletResponse ,@RequestBody @Valid ReportRequest request){
        log.info("READ BRANCHES  REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = branchService.getBranches(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }

    @PostMapping("/pending/read")
    public ApiResponse readPendingBranches(HttpServletResponse httpServletResponse ,@RequestBody @Valid ReportRequest request){
        log.info("READ PENDING BRANCHES  REQUEST :: {}", new Gson().toJson(request));
        ApiResponse response = branchService.getBranchesPendingApproval(request,httpServletResponse);
        log.info("RESPONSE: {}", response);
        return  response;
    }
}
