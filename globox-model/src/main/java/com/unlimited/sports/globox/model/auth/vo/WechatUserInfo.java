package com.unlimited.sports.globox.model.auth.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 微信用户信息VO
 * 用于存储从微信API获取的用户标识信息
 *
 * @author Wreckloud
 * @since 2025/12/20
 */
@Data
public class WechatUserInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户在当前小程序/App中的唯一标识
     */
    private String openid;

    /**
     * 用户在同一微信开放平台账号下的唯一标识（可能为null）
     */
    private String unionid;

    /**
     * 会话密钥（用于解密用户信息，不需要存储）
     */
    private String sessionKey;
}

