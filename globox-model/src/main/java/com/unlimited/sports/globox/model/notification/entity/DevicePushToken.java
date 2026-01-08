package com.unlimited.sports.globox.model.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.common.enums.user.DeviceOsEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备推送Token映射表
 * 用于存储userId与设备Token(RegistrationID)的对应关系
 * 支持通过userId精准推送到设备
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("device_push_tokens")
public class DevicePushToken {

    /**
     * 主键ID
     */
    @TableId(value = "device_push_token_id", type = IdType.AUTO)
    private Long devicePushTokenId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户类型
     */
    @TableField("user_type")
    private String userType;

    /**
     * 设备唯一标识
     */
    @TableField("device_id")
    private String deviceId;

    /**
     * 设备推送Token (RegistrationID)
     * 在腾讯云IM+Push集成场景中，RegistrationID = userId
     */
    @TableField("device_token")
    private String deviceToken;

    /**
     * 设备操作系统
     */
    @TableField("device_os")
    private DeviceOsEnum deviceOs;

    /**
     * 是否激活
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
