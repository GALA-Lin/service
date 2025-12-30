package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 微信登录请求
 *
 * @author Wreckloud
 * @since 2025/12/20
 */
@Data
@Schema(description = "微信登录请求")
public class WechatLoginRequest {

    @NotBlank(message = "微信授权code不能为空")
    @Schema(description = "微信授权code（通过uni.login获取）", example = "021abc123def456")
    private String code;
}

