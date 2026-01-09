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
     * @param clientType 客户端类型（third-party-jsapi → miniapp，app → uniapp）
     * @return 微信用户信息（openid、unionid、sessionKey）
     */
    WechatUserInfo getOpenIdAndUnionId(String code, String clientType);

    /**
     * 获取微信用户手机号
     *
     * @param wxCode 微信登录code（用于调用方获取openid/unionid）
     * @param phoneCode 手机号授权code（通过getPhoneNumber获取）
     * @param clientType 客户端类型（third-party-jsapi → miniapp，app → uniapp）
     * @return 手机号
     */
    String getPhoneNumber(String wxCode, String phoneCode, String clientType);
}
