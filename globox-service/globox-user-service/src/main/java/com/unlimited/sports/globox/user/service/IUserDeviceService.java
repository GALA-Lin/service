package com.unlimited.sports.globox.user.service;

import com.unlimited.sports.globox.model.auth.dto.DeviceRegisterRequest;
import com.unlimited.sports.globox.model.auth.entity.UserDevices;

/**
 * 用户设备服务接口
 */
public interface IUserDeviceService {

    /**
     * 注册设备（登录时调用）
     * 1. 保存/更新设备信息
     * 2. 实现单点登录（将其他设备设为不活跃）
     * 3. 发送 MQ 消息到通知服务
     */
    UserDevices registerDevice(DeviceRegisterRequest request);

    /**
     * 注销设备（登出时调用）
     */
    boolean logoutDevice(Long userId, String deviceId);
}
