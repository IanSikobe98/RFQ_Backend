package com.kingdom_bank.RFQBackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kingdom_bank.RFQBackend.config.security.SecurityUser;
import com.kingdom_bank.RFQBackend.dto.*;
import com.kingdom_bank.RFQBackend.entity.*;
import com.kingdom_bank.RFQBackend.enums.ApiResponseCode;
import com.kingdom_bank.RFQBackend.enums.EntityActions;
import com.kingdom_bank.RFQBackend.repository.BranchRepo;
import com.kingdom_bank.RFQBackend.repository.BranchTempRepo;
import com.kingdom_bank.RFQBackend.util.CommonTasks;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.reactor.IOSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.kingdom_bank.RFQBackend.enums.Action.APPROVE;
import static com.kingdom_bank.RFQBackend.enums.Action.REJECT;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BranchService {
    private final BranchRepo branchRepo;
    private final ConstantUtil constantUtil;
    private final BranchTempRepo branchTempRepo;
    private final CommonTasks commonTasks;

    /**
     * Function to get the Authenticated user that was authenticated using JWT
     * @return ApiUser: The authenticated user
     */
    private User getauthenticatedAPIUser(){
        return  ((SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
    }


    public ApiResponse createBranch(BranchRequest request, HttpServletResponse httpServletResponse) {
        ApiResponse response = new ApiResponse();
        log.info("Creating branch with request {}", request);
        try{
            User user = getauthenticatedAPIUser();
            Optional<Branch> branch = branchRepo.findByBranchNameAndStatusId(request.getBranchName(), constantUtil.ACTIVE);
            if(branch.isPresent()){
                log.info("Branch with name {} already exists", request.getBranchName());
                response.setResponseMessage("Branch with name " + request.getBranchName() + " already exists");
                response.setResponseCode(ApiResponseCode.FAIL);
                return response;
            }

            Optional<Branch> existingBranchCode = branchRepo.findByBranchCodeAndStatusId(request.getBranchName(), constantUtil.ACTIVE);

            if(existingBranchCode.isPresent()){
                log.info("Branch with code {} already exists", request.getBranchName());
                response.setResponseMessage("Branch with code " + request.getBranchName() + " already exists");
                response.setResponseCode(ApiResponseCode.FAIL);
                return response;
            }


            BranchTemp branchTemp = BranchTemp.builder()
                    .branchCode(request.getBranchCode())
                    .branchName(request.getBranchName())
                    .bankCode(request.getBankCode())
                    .createdBy(user)
                    .updatedBy(user)
                    .action(EntityActions.CREATE.getValue())
                    .status(constantUtil.PENDING_APPROVAL)
                    .build();

            branchTempRepo.saveAndFlush(branchTemp);

            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Branch successfully created.Awaiting Approval");
            log.info("Branch successfully created of request {}", request);

        }
        catch (Exception e) {
            log.error("ERROR OCCURRED DURING CREATION OF BRANCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while creating Branch");
        }
        return response;
    }


    public ApiResponse editBranch(BranchRequest request, HttpServletResponse httpServletResponse) {
        log.info("Editing branch with request {}", request);
        ApiResponse response = new ApiResponse();
        try{
            User user = getauthenticatedAPIUser();
            log.info("Check if user exists  for id ..... {}",request.getId());
            Optional<Branch> existingBranch= branchRepo.findById(request.getId());

            if(existingBranch.isEmpty()){
                log.info("Branch with id {} does not exist", request.getId());
                response.setResponseMessage("Branch selected does not exist");
                response.setResponseCode(ApiResponseCode.FAIL);
                return response;
            }

            Branch branch = existingBranch.get();
            Optional<Branch> existingBranchNames= branchRepo.findByBranchNameAndStatusId(request.getBranchName(), constantUtil.ACTIVE);
            if(existingBranchNames.isPresent()){
                log.info("Branch with name {} already exists", request.getBranchName());
                response.setResponseMessage("Branch with name " + request.getBranchName() + " already exists");
                response.setResponseCode(ApiResponseCode.FAIL);
                return response;
            }

            Optional<Branch> existingBranchCode = branchRepo.findByBranchCodeAndStatusId(request.getBranchName(), constantUtil.ACTIVE);

            if(existingBranchCode.isPresent()){
                log.info("Branch with code {} already exists", request.getBranchName());
                response.setResponseMessage("Branch with code " + request.getBranchName() + " already exists");
                response.setResponseCode(ApiResponseCode.FAIL);
                return response;
            }


            BranchTemp branchTemp = BranchTemp.builder()
                    .branchCode(request.getBranchCode())
                    .branchName(request.getBranchName())
                    .bankCode(request.getBankCode())
                    .branch(branch)
                    .createdBy(user)
                    .updatedBy(user)
                    .action(EntityActions.EDIT.getValue())
                    .status(constantUtil.PENDING_APPROVAL)
                    .build();

            branchTempRepo.saveAndFlush(branchTemp);

            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Branch update Request created successfully");
            log.info("Branch update Request created successfully for request {}", request);

        }

        catch (Exception e) {
            log.error("ERROR OCCURRED DURING UPDATING OF BRANCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while updating branch");
        }
        return response;
    }


    public ApiResponse approveOrRejectBranch(ApprovalRequest request, User loggedInUser, Integer id){
        ApiResponse response = new ApiResponse();
        log.info("Approving branch of id {}...",id);
        try{
            Optional<BranchTemp> existingBranchOptional = branchTempRepo.findById(id);
            if (existingBranchOptional.isEmpty()) {
                response.setResponseCode(ApiResponseCode.FAIL);
                response.setResponseMessage("Branch  with id  "+ id+ " does not exist");
                return response;
            }
            BranchTemp existingBranch = existingBranchOptional.get();
            BranchRequest branchRequest = BranchRequest.builder().id(id).build();
            branchRequest.setComment(request.getDescription());

            if(request.getAction().equals(APPROVE.getValue())){
                if(existingBranch.getAction().equalsIgnoreCase(EntityActions.CREATE.getValue())) {
                    Branch branch = Branch.builder()
                            .bankCode(existingBranch.getBankCode())
                            .branchName(existingBranch.getBranchName())
                            .branchCode(existingBranch.getBranchCode())
                            .createdBy(existingBranch.getCreatedBy().getUsername())
                            .updatedBy(existingBranch.getUpdatedBy().getUsername())
                            .dateCreated(new Date())
                            .dateUpdated(new Date())
                            .statusId(constantUtil.ACTIVE)
                            .build();

                    branchRepo.saveAndFlush(branch);

                    log.info("Branch successfully Approved");
                    response.setResponseMessage("Branch successfully Approved");
                }
                else if(existingBranch.getAction().equalsIgnoreCase(EntityActions.EDIT.getValue())) {
                    Branch currentBranch = existingBranch.getBranch();

                    if(existingBranch.getBranchName()!=null && !existingBranch.getBranchName().isEmpty()){
                        currentBranch.setBranchName(existingBranch.getBranchName());
                    }
                    if(existingBranch.getBranchCode()!=null && !existingBranch.getBranchCode().isEmpty()){
                        currentBranch.setBranchCode(existingBranch.getBranchCode());
                    }
                    if(existingBranch.getBankCode()!=null && !existingBranch.getBankCode().isEmpty()){
                        currentBranch.setBankCode(existingBranch.getBankCode());
                    }

                    currentBranch.setDateUpdated(new Date());
                    currentBranch.setUpdatedBy(existingBranch.getUpdatedBy().getUsername());
                    branchRepo.save(currentBranch);

                    log.info("Branch {} edit update successfully  approved",existingBranch.getId());
                    response.setResponseMessage("Branch update successfully Approved.");
                }
                response.setResponseCode(ApiResponseCode.SUCCESS);
                existingBranch.setDateApproved(new Date());
                existingBranch.setApprovedBy(loggedInUser.getUsername());
                existingBranch.setStatus(constantUtil.ACTIVE);
                branchTempRepo.save(existingBranch);
            }

            else if(request.getAction().equals(REJECT.getValue())){
                existingBranch.setDateApproved(new Date());
                existingBranch.setApprovedBy(loggedInUser.getUsername());
                existingBranch.setStatus(constantUtil.REJECTED);
                existingBranch.setComment(request.getDescription());
                branchTempRepo.save(existingBranch);

                log.info("Branch {} successfully  rejected",existingBranch.getId());
                response.setResponseMessage("Branch record successfully Rejected.");
                response.setResponseCode(ApiResponseCode.SUCCESS);
            }
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING APPROVAL OF BRANCH: {}" ,e.getMessage());
            e.printStackTrace();
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred during approval of branch");
        }
        return response;
    }

    public ReportResponse getBranches(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<Branch> branchList = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();

            if (request.getStatuses() != null  && !request.getStatuses().isEmpty()) {
                branchList = branchRepo.findByStatusId_StatusIdInOrderByDateCreatedDesc(request.getStatuses());
            } else {
                branchList = branchRepo.findAll(Sort.by(Sort.Direction.DESC, "dateCreated"));
            }


            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Branches successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(branchList), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING BRANCH DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the branch Data");
        }
        return response;

    }

    public ReportResponse getBranchesPendingApproval(ReportRequest request, HttpServletResponse httpServletResponse){
        ReportResponse response = new ReportResponse();
        List<BranchTemp> branchList = new ArrayList<>();
        int page = request.getPage();
        int size = request.getSize();
        PageRequest pageable = null;

        try{
            User loggedInUser = getauthenticatedAPIUser();

            if (request.getStatuses() != null  && !request.getStatuses().isEmpty()) {
                branchList = branchTempRepo.findByStatus_StatusIdInOrderByDateCreatedDesc(request.getStatuses());
            } else {
                branchList = branchTempRepo.findAll(Sort.by(Sort.Direction.DESC, "dateCreated"));
            }

//            branchList.forEach(branchTemp -> {
//                Status status = commonTasks.getStatus(branchTemp.getEntityStatus());
//                if(!Objects.isNull(status)){
//                    branchTemp.setEntityStatusName(status.getStatusName());
//                }
//            });


            response.setResponseCode(ApiResponseCode.SUCCESS);
            response.setResponseMessage("Branches successfully fetched");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            response.setData(mapper.readValue(mapper.writeValueAsString(branchList), ArrayList.class));
            return response;


        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING BRANCH DATA FETCH:: {}" ,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while fetching the branch Data");
        }
        return response;

    }
}
