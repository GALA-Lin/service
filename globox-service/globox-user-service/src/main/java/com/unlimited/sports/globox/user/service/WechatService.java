package com.unlimited.sports.globox.user.service;

import com.unlimited.sports.globox.model.auth.vo.WechatUserInfo;

/**
 * 微信服务接口
 *
 * @author Wreckloud
 * @since 2025/12/20
 */
public interface WechatService {

    /**
     * 调用微信API换取openid和unionid
     *
     * @param code 微信授权code
     * @return 微信用户信息（openid、unionid、sessionKey）
     */
    WechatUserInfo getOpenIdAndUnionId(String code);
}
