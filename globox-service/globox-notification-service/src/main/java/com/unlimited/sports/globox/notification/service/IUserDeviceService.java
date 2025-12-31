package com.unlimited.sports.globox.notification.service;

import com.unlimited.sports.globox.model.notification.dto.DeviceRegisterRequest;
import com.unlimited.sports.globox.model.notification.entity.UserDevices;

/**
 * 用户设备服务接口
 */
public interface IUserDeviceService {

    /**
     * 注册设备
     */
    UserDevices registerDevice(DeviceRegisterRequest request);

    /**
     * 注销设备
     */
    boolean logoutDevice(Long userId, String deviceId);



    /**
     * 启用推送
     */
    boolean enablePush(Long userId);

    /**
     * 禁用推送
     */
    boolean disablePush(Long userId);
}
