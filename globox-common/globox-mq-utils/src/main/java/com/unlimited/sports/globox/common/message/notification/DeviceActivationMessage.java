package com.unlimited.sports.globox.common.message.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 设备激活消息
 * 用户登录时由用户服务发送到通知服务
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceActivationMessage implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户类型
     */
    private Integer userType;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 设备Token（RegistrationID）
     */
    private String deviceToken;

    /**
     * 设备操作系统: 1=iOS, 2=Android, 3=HarmonyOS
     */
    private Integer deviceOs;

    /**
     * 消息ID（用于追踪和去重）
     */
    private String messageId;

    /**
     * 时间戳
     */
    private Long timestamp;
}
