package com.unlimited.sports.globox.venue.adapter.dto.d2yun;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * D2yun登录请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class D2yunLoginRequest {

    /**
     * 手机号
     */
    private String phone;

    /**
     * 密码
     */
    private String password;

    /**
     * 平台（PC/MOBILE等）
     */
    private String platform;
}
