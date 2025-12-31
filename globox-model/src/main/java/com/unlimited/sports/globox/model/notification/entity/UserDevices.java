package com.unlimited.sports.globox.model.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.unlimited.sports.globox.model.notification.enums.DeviceOsEnum;
import com.unlimited.sports.globox.model.notification.enums.UserTypeEnum;
import com.unlimited.sports.globox.model.base.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户设备对应表
 * 单登录模式：每个用户同时只有一个is_active=1的设备
 */
@TableName(value = "user_devices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDevices  implements Serializable {

    /**
     * 设备表自增主键
     */
    @TableId(value = "user_devices_id", type = IdType.AUTO)
    private Long userDevicesId;

    /**
     * 用户ID（业务字段）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户类型：CONSUMER(消费者), MERCHANT(商家), COACH(教练)
     */
    @TableField("user_type")
    private UserTypeEnum userType;

    /**
     * 设备唯一标识（硬件相关），用户换手机时会改变
     */
    @TableField("device_id")
    private String deviceId;

    /**
     * 腾讯云registrationId，推送凭证
     */
    @TableField("device_token")
    private String deviceToken;

    /**
     * 设备型号，如iPhone 16
     */
    @TableField("device_model")
    private String deviceModel;

    /**
     * 操作系统：iOS/Android
     */
    @TableField("device_os")
    private DeviceOsEnum deviceOs;

    /**
     * APP版本号
     */
    @TableField("app_version")
    private String appVersion;

    /**
     * 是否活跃：0=不活跃(待激活/已登出)，1=活跃(当前登录)
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 最后活跃时间
     */
    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;

    /**
     * 用户是否启用推送：0=禁用，1=启用
     */
    @TableField("push_enabled")
    private Boolean pushEnabled;


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("created_at")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("updated_at")
    private LocalDateTime updatedAt;


    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
