package com.unlimited.sports.globox.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.message.notification.NotificationMessage;
import com.unlimited.sports.globox.model.notification.entity.DevicePushToken;
import com.unlimited.sports.globox.model.notification.entity.NotificationTemplates;
import com.unlimited.sports.globox.model.notification.entity.PushRecords;
import com.alibaba.fastjson2.JSON;
import com.unlimited.sports.globox.common.enums.notification.PushStatusEnum;
import com.unlimited.sports.globox.notification.client.TencentCloudImClient;
import com.unlimited.sports.globox.notification.dto.request.BatchPushRequest;
import com.unlimited.sports.globox.notification.dto.request.OfflinePushInfo;
import com.unlimited.sports.globox.notification.mapper.NotificationTemplatesMapper;
import com.unlimited.sports.globox.notification.service.IDeviceTokenService;
import com.unlimited.sports.globox.notification.service.INotificationService;
import com.unlimited.sports.globox.notification.service.IPushRecordsService;
import com.unlimited.sports.globox.notification.util.TemplateRenderer;
import com.unlimited.sports.globox.notification.enums.MessageTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知服务实现
 * 负责消息处理、模板渲染、推送和结果记录
 */
@Slf4j
@Service
public class NotificationServiceImpl implements INotificationService {

    @Autowired
    private NotificationTemplatesMapper templateMapper;

    @Autowired
    private IPushRecordsService pushRecordsService;

    @Autowired
    private IDeviceTokenService deviceTokenService;

    @Autowired
    private TencentCloudImClient tencentCloudImClient;


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
                log.error("[通知处理] 消息验证失败: message={}", message);
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
        // 提取所有接收者的userId
        List<Long> userIds = recipients.stream()
                .map(NotificationMessage.Recipient::getUserId)
                .toList();

        // 批量查询所有用户的最新活跃设备
        Map<Long, DevicePushToken> userDeviceMap = deviceTokenService.getLatestActiveDevicesByUserIds(userIds);

        // 收集所有有效的设备信息和被过滤的记录
        List<String> deviceTokens = new ArrayList<>();
        Map<String, DevicePushInfo> deviceInfoMap = new HashMap<>();

        // 按是否有活跃设备分组
        Map<Boolean, List<NotificationMessage.Recipient>> partitioned = recipients.stream()
                .collect(Collectors.partitioningBy(recipient -> userDeviceMap.containsKey(recipient.getUserId())));

        // 处理无活跃设备的用户（构建过滤记录）
        List<PushRecords> filteredRecords = partitioned.get(false).stream()
                .peek(recipient -> log.warn("[批量推送] 用户无活跃设备: userId={}", recipient.getUserId()))
                .map(recipient -> {
                    PushRecords record = buildPushRecord(
                            messageId, messageType, recipient.getUserId(), null, null,
                            renderedTitle, renderedContent, action, variables,
                            null, PushStatusEnum.FILTERED, template, null
                    );
                    record.setErrorMsg("用户无活跃设备");
                    return record;
                })
                .toList();

        // 处理有活跃设备的用户（添加到批量推送列表）
        partitioned.get(true).forEach(recipient -> {
            DevicePushToken device = userDeviceMap.get(recipient.getUserId());
            String deviceToken = device.getDeviceToken();
            // 过滤掉无效的deviceToken
            if (deviceToken == null || deviceToken.trim().isEmpty()) {
                log.warn("[批量推送] deviceToken无效: userId={}, deviceId={}", recipient.getUserId(), device.getDeviceId());
                return;
            }

            deviceTokens.add(deviceToken);
            deviceInfoMap.put(deviceToken, new DevicePushInfo(
                    recipient.getUserId(), device.getDeviceId(), deviceToken, device.getUserType()
            ));
        });

        // 批量插入被过滤的推送记录
        if (!filteredRecords.isEmpty()) {
            pushRecordsService.saveBatchRecords(filteredRecords);
        }

        // 批量发送推送
        if (!deviceTokens.isEmpty()) {
            log.info("[批量推送] 开始批量推送: 设备数量={}", deviceTokens.size());
            List<PushRecords> recordsToSave = new ArrayList<>();

            try {
                // 构建离线推送信息
                // ext字段必须是有效的JSON字符串，不能为null，用{}表示空对象
                String extJson = null;
                Map<String, Object> extData = new HashMap<>(variables);
                // 将 notificationId 添加到 ext，前端可直接从推送数据中获取
                extData.put("notificationId", messageId);
                if (action != null) {
                    extData.put("action", action);
                }

                // 添加消息类型（探索/球局/系统），便于前端做对应的UI展示或跳转
                if (template != null && template.getNotificationModule() != null) {
                    MessageTypeEnum userMsgType = MessageTypeEnum.fromModuleCode(template.getNotificationModule());
                    if (userMsgType != null) {
                        extData.put("messageType", userMsgType.getCode());
                        log.debug("[批量推送] 消息分类: module={}, messageType={}", template.getNotificationModule(), userMsgType.getCode());
                    }
                }

                extJson = JSON.toJSONString(extData);

                OfflinePushInfo offlinePushInfo = OfflinePushInfo.builder()
                        .pushFlag(0)  // 0=启用离线推送（用户在线和离线都会收到消息）
                        .title(renderedTitle)
                        .desc(renderedContent)
                        .ext(extJson)
                        .build();

                // 构建批量推送请求
                BatchPushRequest request = BatchPushRequest.builder()
                        .toAccount(deviceTokens)
                        .offlinePushInfo(offlinePushInfo)
                        .build();

                // 发送推送
                String taskId = tencentCloudImClient.batchPush(request);

                if (taskId != null) {
                    log.info("[批量推送] 推送成功: taskId={}, 设备数量={}", taskId, deviceTokens.size());
                    buildBatchPushRecords(deviceTokens, deviceInfoMap, recordsToSave, messageId, messageType,
                            renderedTitle, renderedContent, action, variables, template, taskId, PushStatusEnum.SENT, null);
                } else {
                    log.error("[批量推送] 推送失败");
                    buildBatchPushRecords(deviceTokens, deviceInfoMap, recordsToSave, messageId, messageType,
                            renderedTitle, renderedContent, action, variables, template, null, PushStatusEnum.FAILED, "腾讯云批量推送失败");
                }

            } catch (Exception e) {
                log.error("[批量推送] 发送异常", e);
                buildBatchPushRecords(deviceTokens, deviceInfoMap, recordsToSave, messageId, messageType,
                        renderedTitle, renderedContent, action, variables, template, null, PushStatusEnum.FAILED, e.getMessage());
            }

            // 批量插入所有推送记录
            if (!recordsToSave.isEmpty()) {
                pushRecordsService.saveBatchRecords(recordsToSave);
            }
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
     * 批量构建推送记录
     */
    private void buildBatchPushRecords(List<String> deviceTokens,
                                       Map<String, DevicePushInfo> deviceInfoMap,
                                       List<PushRecords> recordsToSave,
                                       String messageId, String messageType,
                                       String renderedTitle, String renderedContent,
                                       String action, Map<String, Object> variables,
                                       NotificationTemplates template,
                                       String taskId, PushStatusEnum status, String errorMsg) {
        for (String deviceToken : deviceTokens) {
            DevicePushInfo info = deviceInfoMap.get(deviceToken);
            PushRecords record = buildPushRecord(
                    messageId, messageType, info.userId, info.deviceId, deviceToken,
                    renderedTitle, renderedContent, action, variables, taskId,
                    status, template, info.userType
            );

            // 设置成功时间或错误信息
            if (status == PushStatusEnum.SENT) {
                record.setSentAt(LocalDateTime.now());
            } else if (errorMsg != null) {
                record.setErrorMsg(errorMsg);
            }

            recordsToSave.add(record);
        }
    }

    /**
     * 构建推送记录
     */
    private PushRecords buildPushRecord(String messageId, String messageType, Long userId,
                                       String deviceId, String deviceToken,
                                       String title, String content, String action,
                                       Map<String, Object> customData,
                                       String taskId, PushStatusEnum status,
                                       NotificationTemplates template, String userType) {
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
    public PushRecords getPushRecord(String messageId) {
        LambdaQueryWrapper<PushRecords> wrapper = Wrappers.lambdaQuery(PushRecords.class)
                .eq(PushRecords::getNotificationId, messageId);

        return pushRecordsService.getOne(wrapper);
    }


    /**
     * 设备推送信息（用于批量推送记录）
     */
    private static class DevicePushInfo {
        Long userId;
        String deviceId;
        String deviceToken;
        String userType;

        DevicePushInfo(Long userId, String deviceId, String deviceToken, String userType) {
            this.userId = userId;
            this.deviceId = deviceId;
            this.deviceToken = deviceToken;
            this.userType = userType;
        }
    }

}
