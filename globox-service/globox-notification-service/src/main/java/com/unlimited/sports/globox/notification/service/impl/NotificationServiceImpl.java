package com.unlimited.sports.globox.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.message.notification.NotificationMessage;
import com.unlimited.sports.globox.model.notification.entity.DevicePushToken;
import com.unlimited.sports.globox.model.notification.entity.NotificationTemplates;
import com.unlimited.sports.globox.model.notification.entity.PushRecords;
import com.unlimited.sports.globox.common.enums.notification.PushStatusEnum;
import com.unlimited.sports.globox.notification.client.TencentCloudClient;
import com.unlimited.sports.globox.notification.mapper.NotificationTemplatesMapper;
import com.unlimited.sports.globox.notification.mapper.PushRecordsMapper;
import com.unlimited.sports.globox.notification.service.IDeviceTokenService;
import com.unlimited.sports.globox.notification.service.INotificationService;
import com.unlimited.sports.globox.notification.util.TemplateRenderer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 通知服务实现
 * 负责消息处理、模板渲染、推送和结果记录
 */
@Slf4j
@Service
public class NotificationServiceImpl implements INotificationService {

    @Autowired
    private  TencentCloudClient tencentCloudClient;

    @Autowired
    private  NotificationTemplatesMapper templateMapper;

    @Autowired
    private  PushRecordsMapper pushRecordsMapper;

    @Autowired
    private  IDeviceTokenService deviceTokenService;




    /**
     * 处理通知消息
     * 流程：
     * 1. 验证消息的有效性
     * 2. 检查消息去重（messageId）
     * 3. 获取模板
     * 4. 渲染推送标题和内容
     * 5. 获取用户的活跃设备
     * 6. 发送推送到腾讯云
     * 7. 记录推送结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleNotification(NotificationMessage message) {
        String messageId = message.getMessageId();
        String messageType = message.getMessageType();

        log.info("[通知处理] 开始处理消息: messageId={}, messageType={}", messageId, messageType);

        try {
            // 验证消息有效性
            if (!validateMessage(message)) {
                log.error("[通知处理] 消息验证失败: messageId={}", messageId);
                return;
            }

            //  消息去重 - 检查是否已处理过
            PushRecords existingRecord = getPushRecord(messageId);
            if (existingRecord != null) {
                log.warn("[通知处理] 消息已处理，跳过: messageId={}", messageId);
                return;
            }

            //  获取消息模板
            NotificationTemplates template = getTemplate(messageType);
            if (template == null) {
                log.error("[通知处理] 未找到消息模板: messageType={}", messageType);
                return;
            }

            //  获取模板变量
            Map<String, Object> variables = message.getPayload().getCustomData();
            if (variables == null) {
                variables = new HashMap<>();
            }

            // 渲染推送标题和内容
            String renderedTitle = TemplateRenderer.render(template.getTitleTemplate(), variables);
            String renderedContent = TemplateRenderer.render(template.getContentTemplate(), variables);
            String action = null;
            if (template.getActionTarget() != null) {
                action = TemplateRenderer.render(template.getActionTarget(), variables);
            }

            log.info("[通知处理] 模板渲染完成: title={}, content={}", renderedTitle, renderedContent);

            // 批量处理接收者
            if (message.getRecipients() != null && !message.getRecipients().isEmpty()) {
                processBatchRecipients(
                        messageId, messageType, message.getRecipients(),
                        renderedTitle, renderedContent, action, variables, template
                );
            }

            log.info("[通知处理] 消息处理完成: messageId={}, 接收者数量={}",
                    messageId, message.getRecipients() != null ? message.getRecipients().size() : 0);

        } catch (Exception e) {
            log.error("[通知处理] 处理失败: messageId={}", messageId, e);
            throw new RuntimeException("消息处理失败: " + messageId, e);
        }
    }

    /**
     * 批量处理接收者（收集所有设备token后批量推送）
     */
    private void processBatchRecipients(String messageId, String messageType,
                                       List<NotificationMessage.Recipient> recipients,
                                       String renderedTitle, String renderedContent,
                                       String action, Map<String, Object> variables,
                                       NotificationTemplates template) {
        // 收集所有有效的设备信息
        List<String> deviceTokens = new ArrayList<>();
        Map<String, DevicePushInfo> deviceInfoMap = new HashMap<>();

        for (NotificationMessage.Recipient recipient : recipients) {
            Long userId = recipient.getUserId();

            try {
                // 获取用户的活跃设备列表（单点登录，应该只有一个）
                List<DevicePushToken> devices = deviceTokenService.getActiveDeviceTokens(userId);

                if (devices == null || devices.isEmpty()) {
                    log.warn("[批量推送] 用户无活跃设备: userId={}", userId);
                    recordFilteredPush(
                            messageId, messageType, userId, null, null,
                            renderedTitle, renderedContent, action, variables,
                            PushStatusEnum.FILTERED, null, "用户无活跃设备", template, 0
                    );
                    continue;
                }

                // 添加到批量推送列表（单点登录，取第一个设备）
                DevicePushToken device = devices.get(0);
                String deviceToken = device.getDeviceToken();
                deviceTokens.add(deviceToken);
                deviceInfoMap.put(deviceToken, new DevicePushInfo(userId, device.getDeviceId(), deviceToken, device.getUserType().getCode()));

            } catch (Exception e) {
                log.error("[批量推送] 获取设备失败: userId={}", userId, e);
            }
        }

        // 批量发送推送
        if (!deviceTokens.isEmpty()) {
            log.info("[批量推送] 开始批量推送: 设备数量={}", deviceTokens.size());

            try {
                String taskId = tencentCloudClient.batchPush(deviceTokens, renderedTitle, renderedContent, action, variables);

                if (taskId != null) {
                    log.info("[批量推送] 推送成功: taskId={}, 设备数量={}", taskId, deviceTokens.size());

                    // 为每个设备记录推送结果
                    for (String deviceToken : deviceTokens) {
                        DevicePushInfo info = deviceInfoMap.get(deviceToken);
                        recordSuccessfulPush(
                                messageId, messageType, info.userId, info.deviceId, deviceToken,
                                renderedTitle, renderedContent, action, variables, taskId, template, info.userType
                        );
                    }
                } else {
                    log.error("[批量推送] 推送失败");

                    // 记录失败
                    for (String deviceToken : deviceTokens) {
                        DevicePushInfo info = deviceInfoMap.get(deviceToken);
                        recordFailedPush(
                                messageId, messageType, info.userId, info.deviceId, deviceToken,
                                renderedTitle, renderedContent, action, variables, null, "腾讯云批量推送失败", template, info.userType
                        );
                    }
                }

            } catch (Exception e) {
                log.error("[批量推送] 发送异常", e);

                // 记录异常
                for (String deviceToken : deviceTokens) {
                    DevicePushInfo info = deviceInfoMap.get(deviceToken);
                    recordFailedPush(
                            messageId, messageType, info.userId, info.deviceId, deviceToken,
                            renderedTitle, renderedContent, action, variables, null, e.getMessage(), template, info.userType
                    );
                }
            }
        }
    }

    /**
     * 设备推送信息（用于批量推送记录）
     */
    private static class DevicePushInfo {
        Long userId;
        String deviceId;
        String deviceToken;
        Integer userType;

        DevicePushInfo(Long userId, String deviceId, String deviceToken, Integer userType) {
            this.userId = userId;
            this.deviceId = deviceId;
            this.deviceToken = deviceToken;
            this.userType = userType;
        }
    }

    /**
     * 获取消息模板（根据事件类型 eventType 查询）
     * messageType 直接就是 eventType，如："ORDER_CONFIRMED"
     */
    private NotificationTemplates getTemplate(String messageType) {
        LambdaQueryWrapper<NotificationTemplates> wrapper = Wrappers.lambdaQuery(NotificationTemplates.class)
                .eq(NotificationTemplates::getEventType, messageType)
                .eq(NotificationTemplates::getIsActive, true)
                .last("LIMIT 1");

        NotificationTemplates template = templateMapper.selectOne(wrapper);
        if (template == null) {
            log.error("[模板查询] 未找到模板: eventType={}", messageType);
        }
        return template;
    }

    /**
     * 验证消息有效性
     */
    private boolean validateMessage(NotificationMessage message) {
        if (message == null) {
            return false;
        }
        if (message.getMessageId() == null || message.getMessageId().isEmpty()) {
            log.error("消息ID为空");
            return false;
        }
        if (message.getMessageType() == null || message.getMessageType().isEmpty()) {
            log.error("消息类型为空");
            return false;
        }
        if (message.getRecipients() == null || message.getRecipients().isEmpty()) {
            log.error("接收者列表为空");
            return false;
        }
        return true;
    }

    /**
     * 记录成功的推送
     */
    private void recordSuccessfulPush(String messageId, String messageType, Long userId,
                                     String deviceId, String deviceToken,
                                     String title, String content, String action,
                                     Map<String, Object> customData, String taskId,
                                     NotificationTemplates template, Integer userType) {
        PushRecords record = buildPushRecord(
                messageId, messageType, userId, deviceId, deviceToken,
                title, content, action, customData, taskId, PushStatusEnum.SENT, template, userType
        );
        record.setSentAt(LocalDateTime.now());
        recordPushResult(record);
    }

    /**
     * 记录失败的推送
     */
    private void recordFailedPush(String messageId, String messageType, Long userId,
                                 String deviceId, String deviceToken,
                                 String title, String content, String action,
                                 Map<String, Object> customData, String errorCode, String errorMsg,
                                 NotificationTemplates template, Integer userType) {
        PushRecords record = buildPushRecord(
                messageId, messageType, userId, deviceId, deviceToken,
                title, content, action, customData, null, PushStatusEnum.FAILED, template, userType
        );
        record.setErrorCode(errorCode);
        record.setErrorMsg(errorMsg);
        recordPushResult(record);
    }

    /**
     * 记录被过滤的推送（用户无活跃设备或已禁用推送）
     */
    private void recordFilteredPush(String messageId, String messageType, Long userId,
                                   String deviceId, String deviceToken,
                                   String title, String content, String action,
                                   Map<String, Object> customData,
                                   PushStatusEnum status, String errorCode, String errorMsg,
                                   NotificationTemplates template, Integer userType) {
        PushRecords record = buildPushRecord(
                messageId, messageType, userId, deviceId, deviceToken,
                title, content, action, customData, null, status, template, userType
        );
        record.setErrorCode(errorCode);
        record.setErrorMsg(errorMsg);
        recordPushResult(record);
    }

    /**
     * 构建推送记录
     */
    private PushRecords buildPushRecord(String messageId, String messageType, Long userId,
                                       String deviceId, String deviceToken,
                                       String title, String content, String action,
                                       Map<String, Object> customData,
                                       String taskId, PushStatusEnum status,
                                       NotificationTemplates template, Integer userType) {
        // 从模板中获取分类信息
        Integer notificationModule = template != null ? template.getNotificationModule() : 0;
        Integer userRole = template != null ? template.getUserRole() : 0;

        return PushRecords.builder()
                .notificationId(messageId)
                .notificationModule(notificationModule)
                .userRole(userRole)
                .eventType(messageType)
                .businessId(String.valueOf(
                        Optional.ofNullable(customData)
                                .map(m -> m.get("businessId"))
                                .orElse("")
                ))
                .userId(userId)
                .userType(userType)
                .deviceId(deviceId)
                .deviceToken(deviceToken)
                .title(title)
                .content(content)
                .action(action)
                .customData(customData)
                .status(status)
                .tencentTaskId(taskId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public String sendToTencent(Long userId, String deviceToken, String title, String content,
                               String action, Map<String, Object> customData) {
        log.info("[腾讯云推送] 发送推送: userId={}, title={}", userId, title);
        return tencentCloudClient.pushToSingleDevice(deviceToken, title, content, action, customData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPushResult(PushRecords record) {
        if (record.getRecordId() == null) {
            pushRecordsMapper.insert(record);
            log.info("[推送记录] 插入新记录: messageId={}, status={}", record.getNotificationId(), record.getStatus());
        } else {
            record.setUpdatedAt(LocalDateTime.now());
            pushRecordsMapper.updateById(record);
            log.info("[推送记录] 更新记录: messageId={}, status={}", record.getNotificationId(), record.getStatus());
        }
    }

    @Override
    public PushRecords getPushRecord(String messageId) {
        LambdaQueryWrapper<PushRecords> wrapper = Wrappers.lambdaQuery(PushRecords.class)
                .eq(PushRecords::getNotificationId, messageId);

        return pushRecordsMapper.selectOne(wrapper);
    }
}
