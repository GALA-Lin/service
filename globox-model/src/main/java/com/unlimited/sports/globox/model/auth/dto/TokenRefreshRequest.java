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
    @Schema(description = "Refresh Token，用于刷新 access token。refreshToken 携带 clientType claim，用于单端校验与下线逻辑。App 端必须保留并按约刷新。", example = "eyJhbGciOiJIUzI1NiJ9...", required = true)
    private String refreshToken;
}

