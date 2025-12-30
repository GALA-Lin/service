package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 设置密码请求
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Data
@Schema(description = "设置密码请求")
public class SetPasswordRequest {

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码必须为6-20位")
    @Schema(description = "新密码", example = "123456")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "确认密码必须为6-20位")
    @Schema(description = "确认密码", example = "123456")
    private String confirmPassword;
}
