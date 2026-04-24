package com.kingdom_bank.RFQBackend.service;

import com.kingdom_bank.RFQBackend.config.notifications.NotificationWebSocketHandler;
import com.kingdom_bank.RFQBackend.entity.Notification;
import com.kingdom_bank.RFQBackend.entity.Order;
import com.kingdom_bank.RFQBackend.entity.Role;
import com.kingdom_bank.RFQBackend.entity.User;
import com.kingdom_bank.RFQBackend.enums.NotificationType;
import com.kingdom_bank.RFQBackend.repository.NotificationRepo;
import com.kingdom_bank.RFQBackend.repository.UserRepo;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@Slf4j
public class NotificationService {
    private final ConstantUtil constantUtil;
    private final NotificationRepo notificationRepo;
    private final NotificationWebSocketHandler webSocketHandler;
    private final MailService mailService;
    private final UserRepo userRepo;

    public NotificationService(ConstantUtil constantUtil, NotificationRepo notificationRepo, NotificationWebSocketHandler webSocketHandler, MailService mailService, UserRepo userRepo) {
        this.constantUtil = constantUtil;
        this.notificationRepo = notificationRepo;
        this.webSocketHandler = webSocketHandler;
        this.mailService = mailService;
        this.userRepo = userRepo;
    }

    public void sendUserNotification(String message, Order order, User user, NotificationType  type) {
        Notification notification = Notification.builder()
                .notificationType(type)
                .message(message)
                .order(order)
                .userId(user)
                .role(user.getRole())
                .isRead(false)
                .status(constantUtil.NEW)
                .createdBy("SYSTEM")
                .updatedBy("SYSTEM")
                .build();

        notificationRepo.saveAndFlush(notification);
        log.info("Notification staged successfully {}",notification);


        //send popup notification
        webSocketHandler.sendPayloadToWebSocket(notification,true);

        //send mail notification
        mailService.sendMail(user.getEmail(),type.name(),message);
    }

    public void sendRoleNotification(String message, Order order, Role role, NotificationType  type) {
        Notification notification = Notification.builder()
                .notificationType(type)
                .message(message)
                .order(order)
                .role(role)
                .isRead(false)
                .status(constantUtil.NEW)
                .createdBy("SYSTEM")
                .updatedBy("SYSTEM")
                .build();

        notificationRepo.saveAndFlush(notification);
        log.info("Notification staged successfully {}",notification);


        webSocketHandler.sendPayloadToWebSocket(notification,false);

        //send mail notification
        sendMailToUsersUnderRole(role,type,message);
    }



    public void sendMailToUsersUnderRole(Role role,NotificationType  type,String message) {
        log.info("Sending email to users under role......... {}",role);
        List<User> users = userRepo.findByRole_RoleId(role.getRoleId());
        if(!users.isEmpty()){
            users.forEach(user->{
                mailService.sendMail(user.getEmail(),type.name(),message);
            });
        }
        log.info("Emails sent to users under role {}.....",role);
    }
}
