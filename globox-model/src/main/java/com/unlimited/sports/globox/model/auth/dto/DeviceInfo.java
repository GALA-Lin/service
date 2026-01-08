package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 设备信息（用于登录时注册设备）
 */
@Data
@Schema(description = "设备信息")
public class DeviceInfo {

    @Schema(description = "设备唯一标识", example = "device_abc123")
    private String deviceId;

    @Schema(description = "设备Token（RegistrationID）", example = "token_xyz789")
    private String deviceToken;

    @Schema(description = "设备型号", example = "iPhone 16 Pro")
    private String deviceModel;

    @Schema(description = "设备操作系统：1=iOS, 2=Android, 3=HarmonyOS", example = "1")
    private Integer deviceOs;

    @Schema(description = "APP版本号", example = "1.0.0")
    private String appVersion;
}
