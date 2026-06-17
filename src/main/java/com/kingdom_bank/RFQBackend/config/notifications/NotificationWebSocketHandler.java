package com.kingdom_bank.RFQBackend.config.notifications;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kingdom_bank.RFQBackend.entity.Notification;
import com.kingdom_bank.RFQBackend.entity.User;
import com.kingdom_bank.RFQBackend.repository.NotificationRepo;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    // Store active WebSocket sessions per user
    private final Map<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    // Store active WebSocket sessions per department
    private final Map<String, Set<WebSocketSession>> roleSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final ConstantUtil constantUtil;
    private final NotificationRepo notificationRepo;

    public NotificationWebSocketHandler(ConstantUtil constantUtil, NotificationRepo notificationRepo) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.constantUtil = constantUtil;
        this.notificationRepo = notificationRepo;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        userSessions.computeIfAbsent("TEMP_" + session.getId(), k -> ConcurrentHashMap.newKeySet()).add(session);
        logger.info("SockJS connection established, waiting for userId/roleId (Session: {})", session.getId());
        sendWelcomeMessage(session, "unknown");
    }

    private void sendWelcomeMessage(WebSocketSession session, String userId) {
        try {
            Map<String, Object> welcomeMessage = Map.of(
                    "notificationType", "Welcome",
                    "message", "Connection established successfully",
                    "userId", userId,
                    "sessionId", session.getId(),
                    "timestamp", System.currentTimeMillis()
            );

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcomeMessage)));
        } catch (Exception e) {
            logger.error("Error sending welcome message to user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            logger.debug("📨 Received message from session {}: {}", session.getId(), payload);

            JsonNode jsonNode = objectMapper.readTree(payload);
            if (jsonNode.has("userId")) {
                String userId = jsonNode.get("userId").asText();
                String roleId = jsonNode.has("roleId") ? jsonNode.get("roleId").asText() : null;

                if (userId != null && !userId.trim().isEmpty()) {
                    // Remove from temp storage
                    userSessions.remove("TEMP_" + session.getId());

                    // Always store userId in session attributes
                    session.getAttributes().put("userId", userId);

                    if (roleId != null && !roleId.trim().isEmpty()) {
                        // Role-based registration
                        session.getAttributes().put("roleId", roleId);
                        roleSessions.computeIfAbsent(roleId, k -> ConcurrentHashMap.newKeySet()).add(session);

                        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);

                        logger.info("🔄 Registered session {} with userId: {} and roleId: {}",
                                session.getId(), userId, roleId);

                        // Send confirmation for department registration
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                                "type", "connection_confirmed",
                                "userId", userId,
                                "roleId", roleId,
                                "sessionId", session.getId(),
                                "registrationType", "user"
                        ))));
                    }
                    else{
                        logger.info("User does not have role data");
                    }
//                    else {
                        // User-based registration (existing behavior)
//                        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
//
//                        logger.info("🔄 Registered session {} with userId: {}", session.getId(), userId);
//
//                        // Send confirmation for user registration
//                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
//                                "type", "connection_confirmed",
//                                "userId", userId,
//                                "sessionId", session.getId(),
//                                "registrationType", "user"
//                        ))));
//                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error handling text message from session {}: {}", session.getId(), e.getMessage());
        }
    }

    public void sendPayloadToWebSocket(Notification notification, Boolean isUserNotification) {
//        //Check timeout FIRST - independent of any other condition
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime createdAt = notification.getDateCreated();
//        long minutesSinceCreation = java.time.Duration.between(createdAt, now).toMinutes();
//
//        if (minutesSinceCreation > 30) {
//            // Notification is stale, mark it as sent to avoid reprocessing
//            notification.setNotificationSentFlag('Y');
//            notificationRepository.save(notification);
//            logger.warn("Notification ID {} for department {} is stale ({} minutes old). Marking as sent to avoid reprocessing.",
//                    notification.getId(), roleId, minutesSinceCreation);
//            return; //Exit early - don't process stale notifications
//        }

//        User user = notification.getUserId();
//        // Check if userId has active sessions
//        Set<WebSocketSession> sessions = userSessions.get(String.valueOf(user.getUserId()));
//        if (sessions != null && !sessions.isEmpty()) {
//            // User is in session, mark as sent
//            notification.setStatus(constantUtil.NOTIFICATION_SENT);
//            notificationRepo.save(notification);
//            logger.info("User {} has active session, notification flag set to Y for notification ID: {}",
//                    userId, notification.getId());
//        }

        if (isUserNotification) {
            // Try to send to user
            boolean userSendSuccess = sendNotificationToUser(String.valueOf(notification.getUserId().getUserId()), notification);

            //If department send was successful, mark notification as sent
            if (userSendSuccess) {
                notification.setStatus(constantUtil.NOTIFICATION_SENT);
                notificationRepo.save(notification);
                logger.info("User send successful for notification ID {}, flag set to Y", notification.getId());
            } else {
                logger.info("User send failed for notification ID {}, but notification is still fresh ( minutes old). Will retry in next cycle.",
                        notification.getId());
            }
        } else {
            // Try to send to department
            boolean roleSendSuccess = sendNotificationToRole(String.valueOf(notification.getRole().getRoleId()), notification);

            //If department send was successful, mark notification as sent
            if (roleSendSuccess) {
                notification.setStatus(constantUtil.NOTIFICATION_SENT);
                notificationRepo.save(notification);
                logger.info("Role send successful for notification ID {}, flag set to Y", notification.getId());
            } else {
                logger.info("Role send failed for notification ID {}, but notification is still fresh ( minutes old). Will retry in next cycle.",
                        notification.getId());
            }
        }
    }
    public boolean sendNotificationToRole(String roleId, Notification notification) {
        Set<WebSocketSession> sessions = roleSessions.get(roleId);
        if (sessions != null && !sessions.isEmpty()) {
            String message = convertNotificationToJson(notification);

            sessions.removeIf(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                        String sessionUserId = (String) session.getAttributes().get("userId");
                        logger.debug("Notification sent to role {} user {} via session {}",
                                roleId, sessionUserId, session.getId());
                        return false;
                    } else {
                        logger.debug("Removing closed session {} for role {}", session.getId(), roleId);
                        return true;
                    }
                } catch (Exception e) {
                    logger.error("Error sending notification to role {} via session {}: {}",
                            roleId, session.getId(), e.getMessage());
                    return true;
                }
            });

            // Check if any sessions remain after cleanup (meaning at least one send succeeded)
            boolean atLeastOneSuccess = !sessions.isEmpty();

            if (atLeastOneSuccess) {
                logger.info("Notification sent to role {} via {} active session(s)", roleId, sessions.size());
                return true;
            } else {
                logger.info("All sessions for role {} were closed or failed", roleId);
                return false;
            }
        } else {
            logger.info("No active WebSocket sessions found for role: {}", roleId);
            return false;
        }
    }

    public boolean sendNotificationToUser(String userId, Notification notification) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null && !sessions.isEmpty()) {
            String message = convertNotificationToJson(notification);
            boolean atLeastOneSuccess = false;

            sessions.removeIf(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                        logger.debug("Notification sent to user {} via session {}", userId, session.getId());
                        return false;
                    } else {
                        logger.debug("Removing closed session {} for user {}", session.getId(), userId);
                        return true;
                    }
                } catch (Exception e) {
                    logger.error("Error sending notification to user {} via session {}: {}",
                            userId, session.getId(), e.getMessage());
                    return true;
                }
            });

            // Check if any sessions remain after cleanup
            atLeastOneSuccess = !sessions.isEmpty();

            if (atLeastOneSuccess) {
                logger.info("Notification sent to user {} via {} active session(s)", userId, sessions.size());
                return true;
            } else {
                logger.info("All sessions for user {} were closed or failed", userId);
                return false;
            }
        } else {
            logger.info("No active WebSocket sessions found for user: {}", userId);
            return false;
        }
    }


    private String convertNotificationToJson(Notification notification) {
        try {
            System.out.println("convert notification to json   " + objectMapper.writeValueAsString(notification));
            return objectMapper.writeValueAsString(notification);
        } catch (Exception e) {
            logger.error("Error converting notification to JSON", e);
            return "{\"error\":\"Failed to serialize notification\",\"timestamp\":" + System.currentTimeMillis() + "}";
        }
    }


}
