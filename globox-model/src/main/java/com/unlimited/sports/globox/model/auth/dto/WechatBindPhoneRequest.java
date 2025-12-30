package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 微信绑定手机号请求
 *
 * @author Wreckloud
 * @since 2025/12/20
 */
@Data
@Schema(description = "微信绑定手机号请求")
public class WechatBindPhoneRequest {

    @NotBlank(message = "临时凭证不能为空")
    @Schema(description = "临时凭证（微信登录接口返回）", example = "temp_abc123def456")
    private String tempToken;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
    @Schema(description = "验证码", example = "123456")
    private String code;

    @Schema(description = "微信昵称（可选，前端通过头像昵称填写能力获取）", example = "微信用户")
    private String nickname;

    @Schema(description = "微信头像URL（可选，前端通过头像昵称填写能力获取）", example = "https://...")
    private String avatarUrl;
}

