package com.unlimited.sports.globox.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.model.notification.entity.DevicePushToken;
import com.unlimited.sports.globox.common.enums.user.DeviceOsEnum;
import com.unlimited.sports.globox.common.enums.notification.PushUserTypeEnum;
import com.unlimited.sports.globox.notification.mapper.DevicePushTokenMapper;
import com.unlimited.sports.globox.notification.service.IDeviceTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备推送Token服务实现
 */
@Slf4j
@Service
public class DeviceTokenServiceImpl implements IDeviceTokenService {

    @Autowired
    private DevicePushTokenMapper devicePushTokenMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DevicePushToken syncDeviceToken(Long userId, Integer userType, String deviceId, String deviceToken, Integer deviceOs) {
        log.info("[设备Token同步] 开始同步: userId={}, userType={}, deviceId={}, deviceOs={}", userId, userType, deviceId, deviceOs);

        // 单点登录：先将该用户的所有其他设备标记为不活跃
        deactivateOtherDevices(userId, deviceId);

        // 检查设备是否已存在
        DevicePushToken existingToken = getDeviceToken(userId, deviceId);

        DevicePushToken token;
        if (existingToken != null) {
            // 设备已存在，更新信息并激活
            existingToken.setUserType(PushUserTypeEnum.fromCode(userType));
            existingToken.setDeviceToken(deviceToken);
            existingToken.setDeviceOs(DeviceOsEnum.fromCode(deviceOs));
            existingToken.setIsActive(true);
            existingToken.setUpdatedAt(LocalDateTime.now());
            devicePushTokenMapper.updateById(existingToken);
            token = existingToken;
            log.info("[设备Token同步] 更新现有设备: userId={}, deviceId={}", userId, deviceId);
        } else {
            // 新设备，插入记录
            token = DevicePushToken.builder()
                    .userId(userId)
                    .userType(PushUserTypeEnum.fromCode(userType))
                    .deviceId(deviceId)
                    .deviceToken(deviceToken)
                    .deviceOs(DeviceOsEnum.fromCode(deviceOs))
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
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
     * 将用户的其他设备标记为不活跃（单点登录）
     */
    private void deactivateOtherDevices(Long userId, String currentDeviceId) {
        LambdaUpdateWrapper<DevicePushToken> updateWrapper = Wrappers.lambdaUpdate(DevicePushToken.class)
                .eq(DevicePushToken::getUserId, userId)
                .ne(DevicePushToken::getDeviceId, currentDeviceId)
                .set(DevicePushToken::getIsActive, false)
                .set(DevicePushToken::getUpdatedAt, LocalDateTime.now());

        int rows = devicePushTokenMapper.update(null, updateWrapper);
        if (rows > 0) {
            log.info("[设备Token同步] 已将 {} 个其他设备设置为不活跃", rows);
        }
    }
}
