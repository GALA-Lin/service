package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Apple登录请求
 *
 */
@Data
@Schema(description = "Apple登录请求")
public class AppleLoginRequest {

    @NotBlank(message = "Apple identityToken不能为空")
    @Schema(description = "Apple identityToken（通过Apple登录获取）", example = "eyJraWQiOiJXNldjT0tCIiwiYWxnIjoiUlMyNTYifQ...", required = true)
    private String identityToken;

    @Schema(description = "设备信息")
    private DeviceInfo deviceInfo;
}

