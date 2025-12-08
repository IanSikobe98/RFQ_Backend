package com.kingdom_bank.RFQBackend.service;


import com.kingdom_bank.RFQBackend.config.security.SecurityUser;
import com.kingdom_bank.RFQBackend.dto.ApiResponse;
import com.kingdom_bank.RFQBackend.dto.ApprovalRequest;
import com.kingdom_bank.RFQBackend.entity.User;
import com.kingdom_bank.RFQBackend.enums.ApiResponseCode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import static com.kingdom_bank.RFQBackend.enums.ApprovalType.USER;


@Service
@Slf4j
@RequiredArgsConstructor
public class ApprovalService {

    private final UserService userService;
    private final Environment environment;
    private ExecutorService executorService;




    @PostConstruct
    public void init() {
        int maxThreads = Integer.parseInt(Objects.requireNonNull(environment.getProperty("threads.approvals.max-threads")));
        this.executorService = Executors.newFixedThreadPool(maxThreads);
    }


    /**
     * Function to get the Authenticated user that was authenticated using JWT
     * @return ApiUser: The authenticated user
     */
    private User getauthenticatedAPIUser(){
        return  ((SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
    }

    public ApiResponse approveOrReject(ApprovalRequest request, HttpServletResponse httpServletResponse){
        ApiResponse response = new ApiResponse();
        List<Future<ApiResponse>> futures = new ArrayList<>();
        try {
            User user = getauthenticatedAPIUser();
            List<String> ids = request.getIds();
            switch (request.getApprovalType()) {
                case USER:
                    for (String id : ids) {
                        Callable<ApiResponse> task = () -> userService.approveOrRejectUser(request,user, Integer.valueOf(id));
                        futures.add(executorService.submit(task));
                    }
                    break;
                case ROLE:
                    for (String id : ids) {
                        Callable<ApiResponse> task = () -> userService.approveOrRejectRole(request,user, Integer.valueOf(id));
                        futures.add(executorService.submit(task));
                    }
                    break;

                default:
                    response.setResponseCode(ApiResponseCode.FAIL);
                    response.setResponseMessage("The approval type is not supported");
                    return response;
            }

            for (Future<ApiResponse> future : futures) {
                try {
                    return future.get();  // Return the response from the first completed task
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error occurred while fetching approval response: " + e.getMessage());
                    httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    log.error(e.getMessage(), e);
                    return ApiResponse.builder()
                            .responseMessage("Failed to Approve")
                            .responseCode(ApiResponseCode.FAIL)
                            .build();
                }
            }

            return ApiResponse.builder()
                    .responseMessage("Failed to Approve")
                    .responseCode(ApiResponseCode.valueOf(ApiResponseCode.FAIL.getCode()))
                    .build();
        }
        catch (Exception e){
            log.error("ERROR OCCURRED DURING APPROVING RECORD FOR REQUEST {}: {}" ,request,e.getMessage());
            e.printStackTrace();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            response.setResponseCode(ApiResponseCode.FAIL);
            response.setResponseMessage("Sorry,Error occurred while approving the record");
        }
        return response;
    }






    @PreDestroy
    public void shutdownExecutorService() {
        log.info("Shutting down ExecutorService...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                    log.error("ExecutorService did not terminate");
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
