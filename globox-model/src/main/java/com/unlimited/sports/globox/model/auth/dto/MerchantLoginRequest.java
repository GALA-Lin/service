package com.unlimited.sports.globox.model.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 商家登录请求
 */
@Data
public class MerchantLoginRequest {

    /**
     * 登录账号
     */
    @NotBlank(message = "账号不能为空")
    private String account;

    /**
     * 登录密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
