package com.unlimited.sports.globox.model.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 设备注册请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegisterRequest {

    /**
     * 用户id(从token解析)
     */
    private Long userId;

    /**
     * 用户角色类型
     */
    @NotBlank(message = "用户角色不能为空")
    private String userType;

    /**
     * 设备唯一标识
     */
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /**
     * 设备Token（RegistrationID）
     */
    @NotBlank(message = "设备Token不能为空")
    private String deviceToken;


    /**
     * 设备型号
     */
    private String deviceModel;

    /**
     * 设备操作系统: 1=iOS, 2=Android, 3=HarmonyOS
     */
    @NotNull(message = "设备操作系统不能为空")
    private Integer deviceOs;

    /**
     * APP版本号
     */
    private String appVersion;
}
