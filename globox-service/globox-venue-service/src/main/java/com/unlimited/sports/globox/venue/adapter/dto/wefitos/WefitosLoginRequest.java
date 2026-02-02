package com.unlimited.sports.globox.venue.adapter.dto.wefitos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wefitos登录请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WefitosLoginRequest {

    /**
     * 用户名（手机号）
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 返回加密数据标志
     */
    private String returnEncData = "";

    /**
     * 随机数
     */
    private String rnd;

    /**
     * 电话号码
     */
    private String phone = "";

    /**
     * 短信验证码
     */
    private String smsCode = "";

    /**
     * 地区号码（+86）
     */
    private String areaNum = "%2B86";
}
