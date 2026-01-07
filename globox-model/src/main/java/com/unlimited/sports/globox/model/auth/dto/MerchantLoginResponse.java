package com.unlimited.sports.globox.model.auth.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

/**
 * 商家登录响应
 */
@Data
@Builder
public class MerchantLoginResponse {

    /**
     * Access Token
     */
    @NonNull
    private String token;

    /**
     * Refresh Token
     */
    @NonNull
    private String refreshToken;

    /**
     * 商家ID
     */
    @NonNull
    private Long merchantId;

    /**
     * 商家账号
     */
    @NonNull
    private String account;

    /**
     * 角色列表
     */
    @NonNull
    private List<String> roles;
}
