package com.unlimited.sports.globox.model.auth.entity;

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
 * 用户设备信息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_devices")
public class UserDevices {

    /**
     * 主键ID
     */
    @TableId(value = "user_devices_id", type = IdType.AUTO)
    private Long userDevicesId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 设备唯一标识
     */
    @TableField("device_id")
    private String deviceId;

    /**
     * 设备Token（RegistrationID）
     */
    @TableField("device_token")
    private String deviceToken;

    /**
     * 设备型号
     */
    @TableField("device_model")
    private String deviceModel;

    /**
     * 设备操作系统
     */
    @TableField("device_os")
    private DeviceOsEnum deviceOs;

    /**
     * APP版本号
     */
    @TableField("app_version")
    private String appVersion;

    /**
     * 是否活跃
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 最后活跃时间
     */
    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;

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
