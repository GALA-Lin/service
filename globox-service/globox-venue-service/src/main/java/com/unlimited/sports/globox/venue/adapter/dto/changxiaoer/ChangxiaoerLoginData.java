package com.unlimited.sports.globox.venue.adapter.dto.changxiaoer;

import lombok.Data;

/**
 * 场小二登录响应数据
 */
@Data
public class ChangxiaoerLoginData {

    /**
     * 管理员ID
     */
    private Integer adminId;

    /**
     * Token
     */
    private String token;
}
