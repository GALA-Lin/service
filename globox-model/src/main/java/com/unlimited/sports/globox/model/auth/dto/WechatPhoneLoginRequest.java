package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 第三方小程序微信手机号登录请求
 *
 * @author Wreckloud
 * @since 2025/12/31
 */
@Data
@Schema(description = "第三方小程序微信手机号登录请求")
public class WechatPhoneLoginRequest {

    @NotBlank(message = "微信授权code不能为空")
    @Schema(description = "微信授权code（通过 wx.login 获取）", example = "021abc123def456", required = true)
    private String wxCode;

    @NotBlank(message = "手机号授权code不能为空")
    @Schema(description = "手机号授权code（通过 getPhoneNumber 获取）", example = "abc123def456", required = true)
    private String phoneCode;

    @Schema(description = "微信昵称（可选）", example = "微信用户")
    private String nickname;

    @Schema(description = "微信头像URL（可选）", example = "https://thirdwx.qlogo.cn/...")
    private String avatarUrl;
}

