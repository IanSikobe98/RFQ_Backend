package com.kingdom_bank.RFQBackend.service;

import com.kingdom_bank.RFQBackend.config.notifications.NotificationWebSocketHandler;
import com.kingdom_bank.RFQBackend.entity.Notification;
import com.kingdom_bank.RFQBackend.entity.Order;
import com.kingdom_bank.RFQBackend.entity.Role;
import com.kingdom_bank.RFQBackend.entity.User;
import com.kingdom_bank.RFQBackend.enums.NotificationType;
import com.kingdom_bank.RFQBackend.repository.NotificationRepo;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Transactional
@Slf4j
public class NotificationService {
    private final ConstantUtil constantUtil;
    private final NotificationRepo notificationRepo;
    private final NotificationWebSocketHandler webSocketHandler;

    public NotificationService(ConstantUtil constantUtil, NotificationRepo notificationRepo, NotificationWebSocketHandler webSocketHandler) {
        this.constantUtil = constantUtil;
        this.notificationRepo = notificationRepo;
        this.webSocketHandler = webSocketHandler;
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


        webSocketHandler.sendPayloadToWebSocket(notification,true);
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
    }
}
