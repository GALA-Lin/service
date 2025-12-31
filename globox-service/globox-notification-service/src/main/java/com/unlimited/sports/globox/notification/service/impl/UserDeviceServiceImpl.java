package com.unlimited.sports.globox.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.model.notification.dto.DeviceRegisterRequest;
import com.unlimited.sports.globox.model.notification.entity.UserDevices;
import com.unlimited.sports.globox.notification.mapper.UserDevicesMapper;
import com.unlimited.sports.globox.notification.service.IUserDeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户设备服务实现
 */
@Slf4j
@Service
public class UserDeviceServiceImpl implements IUserDeviceService {

    @Autowired
    private  UserDevicesMapper userDevicesMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDevices registerDevice(DeviceRegisterRequest request) {
        Long userId = request.getUserId();
        String deviceId = request.getDeviceId();

        log.info("[设备注册] 开始注册设备: userId={}, deviceId={}, os={}",
                userId, deviceId, request.getDeviceOs());

        // 单点登录：先将该用户的所有其他设备标记为不活跃
        deactivateOtherDevices(userId, deviceId);

        // 检查设备是否已存在
        UserDevices existingDevice = getDevice(userId, deviceId);

        UserDevices device;
        if (existingDevice != null) {
            // 设备已存在，更新信息并激活
            existingDevice.setDeviceToken(request.getDeviceToken());
            existingDevice.setDeviceModel(request.getDeviceModel());
            existingDevice.setDeviceOs(request.getDeviceOs());
            existingDevice.setAppVersion(request.getAppVersion());
            existingDevice.setPushEnabled(request.getPushEnabled());
            existingDevice.setIsActive(true);
            existingDevice.setLastActiveAt(LocalDateTime.now());
            existingDevice.setUpdatedAt(LocalDateTime.now());
            userDevicesMapper.updateById(existingDevice);
            device = existingDevice;
        } else {
            // 新设备，插入记录
            device = UserDevices.builder()
                    .userId(userId)
                    .userType(request.getUserType())
                    .deviceId(deviceId)
                    .deviceToken(request.getDeviceToken())
                    .deviceModel(request.getDeviceModel())
                    .deviceOs(request.getDeviceOs())
                    .appVersion(request.getAppVersion())
                    .isActive(true)
                    .pushEnabled(request.getPushEnabled())
                    .lastActiveAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userDevicesMapper.insert(device);
        }
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

        if (rows > 0) {
            log.info("[设备注销] 注销成功: userId={}, deviceId={}", userId, deviceId);
            return true;
        } else {
            log.warn("[设备注销] 注销失败，设备不存在: userId={}, deviceId={}", userId, deviceId);
            return false;
        }
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enablePush(Long userId) {
        log.info("[推送设置] 启用推送: userId={}", userId);

        LambdaUpdateWrapper<UserDevices> updateWrapper = Wrappers.lambdaUpdate(UserDevices.class)
                .eq(UserDevices::getUserId, userId)
                .eq(UserDevices::getIsActive, true)
                .set(UserDevices::getPushEnabled, true)
                .set(UserDevices::getUpdatedAt, LocalDateTime.now());

        int rows = userDevicesMapper.update(null, updateWrapper);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disablePush(Long userId) {
        log.info("[推送设置] 禁用推送: userId={}", userId);

        LambdaUpdateWrapper<UserDevices> updateWrapper = Wrappers.lambdaUpdate(UserDevices.class)
                .eq(UserDevices::getUserId, userId)
                .eq(UserDevices::getIsActive, true)
                .set(UserDevices::getPushEnabled, false)
                .set(UserDevices::getUpdatedAt, LocalDateTime.now());

        int rows = userDevicesMapper.update(null, updateWrapper);
        return rows > 0;
    }

    /**
     * 将用户的其他设备标记为不活跃（单点登录）
     */
    private void deactivateOtherDevices(Long userId, String currentDeviceId) {
        LambdaUpdateWrapper<UserDevices> updateWrapper = Wrappers.lambdaUpdate(UserDevices.class)
                .eq(UserDevices::getUserId, userId)
                .ne(UserDevices::getDeviceId, currentDeviceId)  // 不等于当前设备
                .set(UserDevices::getIsActive, false)
                .set(UserDevices::getUpdatedAt, LocalDateTime.now());
        int rows = userDevicesMapper.update(null, updateWrapper);
        if (rows > 0) {
            log.info("[设备注册] 已注销 {} 个其他设备", rows);
        }
    }
}
