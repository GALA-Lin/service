package com.unlimited.sports.globox.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.constants.NotificationMQConstants;
import com.unlimited.sports.globox.common.enums.user.DeviceOsEnum;
import com.unlimited.sports.globox.common.message.notification.DeviceActivationMessage;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.model.auth.dto.DeviceRegisterRequest;
import com.unlimited.sports.globox.model.auth.entity.UserDevices;
import com.unlimited.sports.globox.user.mapper.UserDevicesMapper;
import com.unlimited.sports.globox.user.service.IUserDeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户设备服务实现
 */
@Slf4j
@Service
public class UserDeviceServiceImpl implements IUserDeviceService {

    @Autowired
    private UserDevicesMapper userDevicesMapper;

    @Autowired
    private MQService mqService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public UserDevices registerDevice(DeviceRegisterRequest request) {
        Long userId = request.getUserId();
        String deviceId = request.getDeviceId();

        log.info("[设备注册] 开始注册设备: userId={}, deviceId={}, os={}",
                userId, deviceId, request.getDeviceOs());

        // 1. 单点登录：先将该用户的所有其他设备标记为不活跃
        deactivateOtherDevices(userId, deviceId);

        // 2. 检查设备是否已存在
        UserDevices existingDevice = getDevice(userId, deviceId);

        UserDevices device;
        LocalDateTime now = LocalDateTime.now();

        if (existingDevice != null) {
            // 设备已存在，更新信息
            device = existingDevice;
            setDeviceInfo(device, request,now);
            userDevicesMapper.updateById(device);
            log.info("[设备注册] 更新现有设备: userId={}, deviceId={}", userId, deviceId);
        } else {
            // 新设备，插入记录
            device = new UserDevices();
            device.setCreatedAt(now);
            setDeviceInfo(device, request, now);
            userDevicesMapper.insert(device);
            log.info("[设备注册] 新增设备: userId={}, deviceId={}", userId, deviceId);
        }

        // 3. 发送 MQ 消息到通知服务，同步设备Token映射
        sendDeviceActivationMessage(device, userId, request.getUserType());

        return device;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean logoutDevice(Long userId, String deviceId) {
        log.info("[设备注销] 开始注销设备: userId={}, deviceId={}", userId, deviceId);

        LambdaUpdateWrapper<UserDevices> updateWrapper = Wrappers.lambdaUpdate(UserDevices.class)
                .eq(UserDevices::getUserId, userId)
                .eq(UserDevices::getDeviceId, deviceId)
                .set(UserDevices::getIsActive, false)
                .set(UserDevices::getUpdatedAt, LocalDateTime.now());

        int rows = userDevicesMapper.update(null, updateWrapper);

        if (rows <= 0) {
            log.warn("[设备注销] 注销失败，设备不存在: userId={}, deviceId={}", userId, deviceId);
            return false;
        }

        log.info("[设备注销] 注销成功: userId={}, deviceId={}", userId, deviceId);
        return true;
    }

    /**
     * 获取设备
     */
    private UserDevices getDevice(Long userId, String deviceId) {
        LambdaQueryWrapper<UserDevices> wrapper = Wrappers.lambdaQuery(UserDevices.class)
                .eq(UserDevices::getUserId, userId)
                .eq(UserDevices::getDeviceId, deviceId)
                .last("LIMIT 1");

        return userDevicesMapper.selectOne(wrapper);
    }

    /**
     * 将用户的其他设备标记为不活跃（单点登录）
     */
    private void deactivateOtherDevices(Long userId, String currentDeviceId) {
        LambdaUpdateWrapper<UserDevices> updateWrapper = Wrappers.lambdaUpdate(UserDevices.class)
                .eq(UserDevices::getUserId, userId)
                .ne(UserDevices::getDeviceId, currentDeviceId)
                .set(UserDevices::getIsActive, false)
                .set(UserDevices::getUpdatedAt, LocalDateTime.now());

        int rows = userDevicesMapper.update(null, updateWrapper);
        if (rows > 0) {
            log.info("[设备注册] 已注销 {} 个其他设备", rows);
        }
    }

    /**
     * 发送设备激活消息到通知服务
     */
    private void sendDeviceActivationMessage(UserDevices device, Long userId, String userType) {
        try {
            DeviceActivationMessage message = DeviceActivationMessage.builder()
                    .userId(userId)
                    .userType(userType)
                    .deviceId(device.getDeviceId())
                    .deviceToken(device.getDeviceToken())
                    .deviceOs(device.getDeviceOs().getCode())
                    .messageId(UUID.randomUUID().toString())
                    .timestamp(System.currentTimeMillis())
                    .build();

            mqService.send(
                    NotificationMQConstants.EXCHANGE_TOPIC_NOTIFICATION,
                    NotificationMQConstants.ROUTING_DEVICE_ACTIVATION,
                    message
            );

            log.info("[设备注册] 已发送设备激活消息到MQ: userId={}, deviceId={}, userType={}", userId, device.getDeviceId(), userType);
        } catch (Exception e) {
            log.error("[设备注册] 发送设备激活消息失败: userId={}, deviceId={}", userId, device.getDeviceId(), e);
            // 不抛出异常，避免影响设备注册流程
        }
    }

    /**
     * 设置设备通用信息
     */
    private void setDeviceInfo(UserDevices device, DeviceRegisterRequest request, LocalDateTime now) {
        device.setUserId(request.getUserId());
        device.setDeviceId(request.getDeviceId());
        device.setDeviceToken(request.getDeviceToken());
        device.setDeviceModel(request.getDeviceModel());
        device.setDeviceOs(DeviceOsEnum.fromCode(request.getDeviceOs()));
        device.setAppVersion(request.getAppVersion());
        device.setIsActive(true);
        device.setLastActiveAt(now);
        device.setUpdatedAt(now);
    }
}
