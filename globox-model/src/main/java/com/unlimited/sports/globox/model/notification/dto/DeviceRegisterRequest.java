package com.unlimited.sports.globox.model.notification.dto;

import com.unlimited.sports.globox.model.notification.enums.DeviceOsEnum;
import com.unlimited.sports.globox.model.notification.enums.UserTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * 设备注册请求
 * 用户登录时调用，激活设备并绑定推送token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户类型
     */
    @NotNull(message = "用户类型不能为空")
    private UserTypeEnum userType;

    /**
     * 设备唯一标识（硬件相关）
     */
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /**
     * 腾讯云registrationId，推送凭证
     */
    @NotBlank(message = "设备Token不能为空")
    private String deviceToken;

    /**
     * 设备型号，如iPhone 16
     */
    private String deviceModel;

    /**
     * 操作系统：iOS/Android
     */
    @NotNull(message = "操作系统不能为空")
    private DeviceOsEnum deviceOs;

    /**
     * APP版本号
     */
    private String appVersion;

    /**
     * 用户是否启用推送：true=启用，false=禁用
     * 默认启用
     */
    @Builder.Default
    private Boolean pushEnabled = true;
}
