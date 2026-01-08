package com.unlimited.sports.globox.venue.adapter.dto.changxiaoer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 场小二登录请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangxiaoerLoginRequest {

    /**
     * 手机号/账号
     */
    private String phone;

    /**
     * 密码
     */
    private String pwd;
}
