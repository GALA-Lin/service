package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Token 刷新请求
 */
@Data
@Schema(description = "Token 刷新请求")
public class TokenRefreshRequest {

    @NotBlank(message = "Refresh Token不能为空")
    @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...", required = true)
    private String refreshToken;
}

