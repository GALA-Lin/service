package com.unlimited.sports.globox.notification.service;

import com.unlimited.sports.globox.model.notification.entity.DevicePushToken;

import java.util.List;

/**
 * 设备推送Token服务接口
 * 负责管理userId与deviceToken的映射关系
 */
public interface IDeviceTokenService {

    /**
     * 同步设备Token（由MQ消费者调用）
     * 如果设备已存在则更新，否则新增
     * 同时将该用户的其他设备设置为不活跃（单点登录）
     *
     * @param userId 用户ID
     * @param userType 用户类型
     * @param deviceId 设备ID
     * @param deviceToken 设备Token（RegistrationID）
     * @param deviceOs 设备操作系统
     * @return 同步后的设备Token记录
     */
    DevicePushToken syncDeviceToken(Long userId, Integer userType, String deviceId, String deviceToken, Integer deviceOs);

    /**
     * 获取用户的活跃设备Token列表（用于推送）
     *
     * @param userId 用户ID
     * @return 设备Token列表
     */
    List<DevicePushToken> getActiveDeviceTokens(Long userId);
}
