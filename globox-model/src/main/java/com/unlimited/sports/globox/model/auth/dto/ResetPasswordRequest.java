package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 找回密码请求
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Data
@Schema(description = "找回密码请求")
public class ResetPasswordRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
    @Schema(description = "验证码", example = "123456")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码必须为6-20位")
    @Schema(description = "新密码", example = "654321")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "确认密码必须为6-20位")
    @Schema(description = "确认密码", example = "654321")
    private String confirmPassword;
}
