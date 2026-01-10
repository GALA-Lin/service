package com.unlimited.sports.globox.common.utils;

import com.unlimited.sports.globox.common.constants.NotificationMQConstants;
import com.unlimited.sports.globox.common.enums.notification.NotificationEventEnum;
import com.unlimited.sports.globox.common.message.notification.NotificationMessage;
import com.unlimited.sports.globox.common.service.MQService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 通知发送工具类
 * 提供通用的通知发送方法，所有服务都可以使用
 */
@Slf4j
@Component
public class NotificationSender {

    @Autowired
    private MQService mqService;

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    /**
     * 发送通知到notification-service
     *
     * @param userIds      接收通知的用户ID列表
     * @param event        通知事件枚举
     * @param businessId   业务ID
     * @param customData   自定义数据（用于模板渲染）
     * @param sourceSystem 来源系统标识（可选，为空则使用配置文件中的应用名）
     */
    public void sendNotification(
            List<Long> userIds,
            NotificationEventEnum event,
            Long businessId,
            Map<String, Object> customData,
            String sourceSystem) {

        if (userIds == null || userIds.isEmpty()) {
            log.warn("[通知发送] 用户ID列表为空 - event={}, businessId={}", event.getEventCode(), businessId);
            return;
        }

        // 如果sourceSystem为空，使用配置文件中的应用名
        String finalSourceSystem = (sourceSystem != null && !sourceSystem.isEmpty())
                ? sourceSystem
                : applicationName;

        // 生成messageId
        String messageId = NotificationMessage.generateMessageId(
                event.getEventCode(),
                userIds.get(0)
        );

        // 构建接收者列表
        List<NotificationMessage.Recipient> recipients = userIds.stream()
                .map(userId -> NotificationMessage.Recipient.builder()
                        .userId(userId)
                        .userType("CONSUMER")
                        .build())
                .toList();

        // 构建通知消息
        NotificationMessage notificationMessage = NotificationMessage.builder()
                .messageId(messageId)
                .messageType(event.name())
                .timestamp(System.currentTimeMillis())
                .sourceSystem(finalSourceSystem)
                .traceId(UUID.randomUUID().toString())
                .recipients(recipients)
                .payload(NotificationMessage.NotificationPayload.builder()
                        .businessId(businessId)
                        .customData(customData)
                        .build())
                .build();

        // 发送消息到notification-service
        mqService.send(
                NotificationMQConstants.EXCHANGE_TOPIC_NOTIFICATION,
                NotificationMQConstants.ROUTING_NOTIFICATION_CORE,
                notificationMessage
        );

        log.info("[通知发送] 消息已发送 - messageId={}, userIds={}, event={}, businessId={}, sourceSystem={}",
                messageId, userIds, event.getEventCode(), businessId, finalSourceSystem);
    }

    /**
     * 发送通知到notification-service（单个用户）
     *
     * @param userId      接收通知的用户ID
     * @param event        通知事件枚举
     * @param businessId   业务ID
     * @param customData   自定义数据（用于模板渲染）
     * @param sourceSystem 来源系统标识（可选，为空则使用配置文件中的应用名）
     */
    public void sendNotification(
            Long userId,
            NotificationEventEnum event,
            Long businessId,
            Map<String, Object> customData,
            String sourceSystem) {

        sendNotification(List.of(userId), event, businessId, customData, sourceSystem);
    }

    /**
     * 发送通知到notification-service（便捷方法）
     * 自动从配置文件获取spring.application.name作为sourceSystem
     *
     * @param userIds      接收通知的用户ID列表
     * @param event        通知事件枚举
     * @param businessId   业务ID
     * @param customData   自定义数据（用于模板渲染）
     */
    public void sendNotification(
            List<Long> userIds,
            NotificationEventEnum event,
            Long businessId,
            Map<String, Object> customData) {

        sendNotification(userIds, event, businessId, customData, null);
    }

    /**
     * 发送通知到notification-service（单个用户，便捷方法）
     * 自动从配置文件获取spring.application.name作为sourceSystem
     *
     * @param userId      接收通知的用户ID
     * @param event        通知事件枚举
     * @param businessId   业务ID
     * @param customData   自定义数据（用于模板渲染）
     */
    public void sendNotification(
            Long userId,
            NotificationEventEnum event,
            Long businessId,
            Map<String, Object> customData) {

        sendNotification(List.of(userId), event, businessId, customData);
    }

    /**
     * 构建通知自定义数据的构建器
     */
    public static class CustomDataBuilder {
        private final Map<String, Object> data = new HashMap<>();

        public CustomDataBuilder put(String key, Object value) {
            data.put(key, value);
            return this;
        }

        public Map<String, Object> build() {
            return data;
        }
    }

    /**
     * 创建自定义数据构建器
     */
    public static CustomDataBuilder createCustomData() {
        return new CustomDataBuilder();
    }
}
