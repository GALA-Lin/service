package com.unlimited.sports.globox.notification.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.enums.notification.NotificationEventEnum;
import com.unlimited.sports.globox.common.enums.notification.PushStatusEnum;
import com.unlimited.sports.globox.common.message.notification.NotificationMessage;
import com.unlimited.sports.globox.model.notification.entity.DevicePushToken;
import com.unlimited.sports.globox.common.enums.user.DeviceOsEnum;
import com.unlimited.sports.globox.model.notification.entity.NotificationTemplates;
import com.unlimited.sports.globox.model.notification.entity.PushRecords;
import com.unlimited.sports.globox.notification.client.TencentCloudImClient;
import com.unlimited.sports.globox.notification.dto.request.BatchPushRequest;
import com.unlimited.sports.globox.notification.dto.request.OfflinePushInfo;
import com.unlimited.sports.globox.notification.mapper.DevicePushTokenMapper;
import com.unlimited.sports.globox.notification.mapper.NotificationTemplatesMapper;
import com.unlimited.sports.globox.notification.service.IDeviceTokenService;
import com.unlimited.sports.globox.notification.service.IPushRecordsService;
import com.unlimited.sports.globox.notification.util.TemplateRenderer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备推送Token服务实现
 */
@Slf4j
@Service
public class DeviceTokenServiceImpl implements IDeviceTokenService {

    @Autowired
    private DevicePushTokenMapper devicePushTokenMapper;

    @Autowired
    private NotificationTemplatesMapper templateMapper;

    @Autowired
    private TencentCloudImClient tencentCloudImClient;

    @Autowired
    private IPushRecordsService pushRecordsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DevicePushToken syncDeviceToken(Long userId, String userType, String deviceId, String deviceToken, Integer deviceOs) {
        log.info("[设备Token同步] 开始同步: userId={}, userType={}, deviceId={}, deviceOs={}", userId, userType, deviceId, deviceOs);

        // 单点登录：先通知旧设备，再将该用户的所有其他设备标记为不活跃
        notifyAndDeactivateOtherDevices(userId, deviceId, deviceOs);

        // 停用同一deviceToken的其他用户（防止设备共享导致的消息误推送）
        deactivateOtherUsersOnSameDevice(deviceToken, userId);

        // 检查设备是否已存在
        DevicePushToken existingToken = getDeviceToken(userId, deviceId);

        DevicePushToken token;
        LocalDateTime now = LocalDateTime.now();

        if (existingToken != null) {
            // 设备已存在，更新信息并激活
            token = existingToken;
            setDeviceTokenInfo(token, userId, userType, deviceId, deviceToken, deviceOs, now);
            devicePushTokenMapper.updateById(token);
            log.info("[设备Token同步] 更新现有设备: userId={}, deviceId={}", userId, deviceId);
        } else {
            // 新设备，插入记录
            token = new DevicePushToken();
            token.setCreatedAt(now);
            setDeviceTokenInfo(token, userId, userType, deviceId, deviceToken, deviceOs, now);
            devicePushTokenMapper.insert(token);
            log.info("[设备Token同步] 新增设备: userId={}, deviceId={}", userId, deviceId);
        }

        return token;
    }

    @Override
    public List<DevicePushToken> getActiveDeviceTokens(Long userId) {
        LambdaQueryWrapper<DevicePushToken> wrapper = Wrappers.lambdaQuery(DevicePushToken.class)
                .eq(DevicePushToken::getUserId, userId)
                .eq(DevicePushToken::getIsActive, true);

        List<DevicePushToken> tokens = devicePushTokenMapper.selectList(wrapper);
        log.debug("[获取活跃设备] userId={}, 活跃设备数={}", userId, tokens.size());
        return tokens;
    }

    @Override
    public Map<Long, DevicePushToken> getLatestActiveDevicesByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        // 批量查询：获取所有用户的活跃设备，按updatedAt倒序
        LambdaQueryWrapper<DevicePushToken> wrapper = Wrappers.lambdaQuery(DevicePushToken.class)
                .in(DevicePushToken::getUserId, userIds)
                .eq(DevicePushToken::getIsActive, true)
                .orderByDesc(DevicePushToken::getUpdatedAt);

        List<DevicePushToken> allTokens = devicePushTokenMapper.selectList(wrapper);

        // 按userId分组，每个用户只保留第一个（即最新的）设备
        Map<Long, DevicePushToken> result = allTokens.stream()
                .collect(Collectors.toMap(
                        DevicePushToken::getUserId,
                        token -> token,
                        (existing, replacement) -> existing  // 保留第一个（最新的）
                ));

        log.debug("[批量获取最新活跃设备] 请求用户数={}, 查询到设备数={}", userIds.size(), result.size());
        return result;
    }

    /**
     * 获取设备Token记录
     */
    private DevicePushToken getDeviceToken(Long userId, String deviceId) {
        LambdaQueryWrapper<DevicePushToken> wrapper = Wrappers.lambdaQuery(DevicePushToken.class)
                .eq(DevicePushToken::getUserId, userId)
                .eq(DevicePushToken::getDeviceId, deviceId)
                .last("LIMIT 1");

        return devicePushTokenMapper.selectOne(wrapper);
    }

    /**
     * 通知并停用用户的其他设备（单点登录）
     * 1. 查询其他活跃设备
     * 2. 发送"账号在别处登录"通知
     * 3. 标记为不活跃
     */
    private void notifyAndDeactivateOtherDevices(Long userId, String currentDeviceId, Integer newDeviceOs) {
        // 查询该用户的其他活跃设备
        LambdaQueryWrapper<DevicePushToken> queryWrapper = Wrappers.lambdaQuery(DevicePushToken.class)
                .eq(DevicePushToken::getUserId, userId)
                .ne(DevicePushToken::getDeviceId, currentDeviceId)
                .eq(DevicePushToken::getIsActive, true);

        List<DevicePushToken> otherDevices = devicePushTokenMapper.selectList(queryWrapper);

        if (otherDevices.isEmpty()) {
            log.info("[设备Token同步] 该用户无其他活跃设备，无需通知");
            return;
        }


        // 获取"账号在别处登录"通知模板
        NotificationTemplates template = getLoginElsewhereTemplate();
        if (template == null) {
            log.warn("[设备Token同步] 未找到ACCOUNT_LOGIN_ELSEWHERE模板，跳过通知直接停用设备");
            deactivateDevices(userId, currentDeviceId);
            return;
        }

        // 准备模板变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("deviceType", DeviceOsEnum.fromCode(newDeviceOs).getDescription());
        variables.put("loginTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 渲染消息
        String renderedTitle = TemplateRenderer.render(template.getTitleTemplate(), variables);
        String renderedContent = TemplateRenderer.render(template.getContentTemplate(), variables);
        String action = null;
        if (template.getActionTarget() != null) {
            action = TemplateRenderer.render(template.getActionTarget(), variables);
        }
        String variablesJsonStr = JSON.toJSONString(variables);
        // 收集有效的设备Token
        List<String> deviceTokens = otherDevices.stream()
                .map(DevicePushToken::getDeviceToken)
                .filter(token -> token != null && !token.trim().isEmpty())
                .collect(Collectors.toList());

        if (deviceTokens.isEmpty()) {
            log.warn("[设备Token同步] 其他设备的deviceToken均无效，跳过推送");
            deactivateDevices(userId, currentDeviceId);
            return;
        }

        // 发送推送
        try {
            String extJson = action != null
                    ? JSON.toJSONString(Map.of("action", action))
                    : "{}";

            OfflinePushInfo offlinePushInfo = OfflinePushInfo.builder()
                    .pushFlag(0)
                    .title(renderedTitle)
                    .desc(renderedContent)
                    .ext(extJson)
                    .build();

            BatchPushRequest request = BatchPushRequest.builder()
                    .toAccount(deviceTokens)
                    .offlinePushInfo(offlinePushInfo)
                    .build();

            String taskId = tencentCloudImClient.batchPush(request);

            // 记录推送结果
            String messageId = NotificationMessage.generateMessageId(
                    NotificationEventEnum.SYSTEM_ACCOUNT_LOGIN_ELSEWHERE.getEventCode(),
                    userId
            );
            List<PushRecords> records = new ArrayList<>();

            for (DevicePushToken device : otherDevices) {
                if (device.getDeviceToken() == null || device.getDeviceToken().trim().isEmpty()) {
                    continue;
                }

                PushRecords record = PushRecords.builder()
                        .notificationId(messageId)
                        .notificationModule(NotificationEventEnum.SYSTEM_ACCOUNT_LOGIN_ELSEWHERE.getModule().getCode())
                        .userRole(NotificationEventEnum.SYSTEM_ACCOUNT_LOGIN_ELSEWHERE.getRole().getCode())
                        .eventType(NotificationEventEnum.SYSTEM_ACCOUNT_LOGIN_ELSEWHERE.getEventCode())
                        .userId(device.getUserId())
                        .userType(device.getUserType())
                        .deviceId(device.getDeviceId())
                        .deviceToken(device.getDeviceToken())
                        .title(renderedTitle)
                        .content(renderedContent)
                        .action(action)
                        .customData(variablesJsonStr)
                        .status(taskId != null ? PushStatusEnum.SENT : PushStatusEnum.FAILED)
                        .tencentTaskId(taskId)
                        .sentAt(taskId != null ? LocalDateTime.now() : null)
                        .errorMsg(taskId == null ? "腾讯云推送失败" : null)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                records.add(record);
            }

            if (!records.isEmpty()) {
                pushRecordsService.saveBatchRecords(records);
            }

            if (taskId != null) {
                log.info("[设备Token同步] 账号登录通知发送成功: taskId={}, 设备数量={}", taskId, deviceTokens.size());
            } else {
                log.error("[设备Token同步] 账号登录通知发送失败");
            }

        } catch (Exception e) {
            log.error("[设备Token同步] 发送账号登录通知异常", e);
        }

        // 停用其他设备
        deactivateDevices(userId, currentDeviceId);
    }

    /**
     * 停用用户的其他设备
     */
    private void deactivateDevices(Long userId, String currentDeviceId) {
        LambdaUpdateWrapper<DevicePushToken> updateWrapper = Wrappers.lambdaUpdate(DevicePushToken.class)
                .eq(DevicePushToken::getUserId, userId)
                .ne(DevicePushToken::getDeviceId, currentDeviceId)
                .set(DevicePushToken::getIsActive, false)
                .set(DevicePushToken::getUpdatedAt, LocalDateTime.now());

        devicePushTokenMapper.update(null, updateWrapper);
    }

    /**
     * 停用同一deviceToken的其他用户
     * 场景：用户A退出登录后，用户B在同一设备登录，需要停用用户A的该设备记录
     */
    private void deactivateOtherUsersOnSameDevice(String deviceToken, Long currentUserId) {
        if (deviceToken == null || deviceToken.trim().isEmpty()) {
            return;
        }

        LambdaUpdateWrapper<DevicePushToken> updateWrapper = Wrappers.lambdaUpdate(DevicePushToken.class)
                .eq(DevicePushToken::getDeviceToken, deviceToken)
                .ne(DevicePushToken::getUserId, currentUserId)
                .eq(DevicePushToken::getIsActive, true)
                .set(DevicePushToken::getIsActive, false)
                .set(DevicePushToken::getUpdatedAt, LocalDateTime.now());

        int updated = devicePushTokenMapper.update(null, updateWrapper);
        if (updated > 0) {
            log.info("[设备Token同步] 停用同设备的其他用户: deviceToken={}, 当前用户={}, 停用数量={}",
                    deviceToken, currentUserId, updated);
        }
    }



    /**
     * 获取"账号在别处登录"模板
     */
    private NotificationTemplates getLoginElsewhereTemplate() {
        LambdaQueryWrapper<NotificationTemplates> wrapper = Wrappers.lambdaQuery(NotificationTemplates.class)
                .eq(NotificationTemplates::getEventType, NotificationEventEnum.SYSTEM_ACCOUNT_LOGIN_ELSEWHERE.getEventCode())
                .eq(NotificationTemplates::getIsActive, true)
                .last("LIMIT 1");

        return templateMapper.selectOne(wrapper);
    }

    /**
     * 设置设备Token通用信息
     */
    private void setDeviceTokenInfo(DevicePushToken token, Long userId, String userType, String deviceId,
                                    String deviceToken, Integer deviceOs, LocalDateTime now) {
        token.setUserId(userId);
        token.setUserType(userType);
        token.setDeviceId(deviceId);
        token.setDeviceToken(deviceToken);
        token.setDeviceOs(DeviceOsEnum.fromCode(deviceOs));
        token.setIsActive(true);
        token.setUpdatedAt(now);
    }
}
